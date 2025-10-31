package com.zhoucable.marketbackend.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import com.zhoucable.marketbackend.modules.address.service.AddressService;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.order.dto.*;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.entity.OrderItem;
import com.zhoucable.marketbackend.modules.order.entity.ParentOrder;
import com.zhoucable.marketbackend.modules.order.mapper.OrderItemMapper;
import com.zhoucable.marketbackend.modules.order.mapper.OrderMapper;
import com.zhoucable.marketbackend.modules.order.mapper.ParentOrderMapper;
import com.zhoucable.marketbackend.modules.order.service.OrderService;
import com.zhoucable.marketbackend.modules.order.vo.*;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.mapper.ProductMapper;
import com.zhoucable.marketbackend.modules.product.mapper.ProductSkuMapper;
import com.zhoucable.marketbackend.modules.product.service.ProductSkuService;
import com.zhoucable.marketbackend.modules.shoppingcart.Service.CartService;
import com.zhoucable.marketbackend.modules.shoppingcart.entity.ShoppingCart;
import com.zhoucable.marketbackend.utils.RedisKeyUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.data.redis.core.ScanCursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;


/**
 * 订单服务实现
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private ParentOrderMapper parentOrderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper; //用于扣减库存

    @Autowired
    private ProductMapper productMapper; //用于获取SPU信息

    @Autowired
    private ObjectMapper objectMapper; //用于JSON转换

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StoreService storeService;

    //订单号生成格式
    private static final DateTimeFormatter ORDER_NUMBER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    //====内部方法====

    /**
     * 扣减库存（核心方法）
     * @param skuId SKU ID
     * @param quantityToDeduct 要扣减的数量
     * @return true 如果扣减成功；false 如果库存不足
     */
    private boolean deductStock(Long skuId, int quantityToDeduct){
        LambdaUpdateWrapper<ProductSku> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ProductSku::getId, skuId)
                .ge(ProductSku::getStock, quantityToDeduct) //检查库存是否足够
                .setSql("stock = stock - " + quantityToDeduct); //执行扣减

        //update 方法也会返回 boolean，具体看版本
        //return productSkuService.update(updateWrapper);
        int affectedRows = productSkuMapper.update(updateWrapper);
        return affectedRows > 0;
    }

    /**
     * 回滚库存（用于下单失败或取消订单时）
     * @param stockRollbackMap Key：skuId，Value：要回滚（增加）的数量
     */
    private void rollBackStock(Map<Long ,Integer> stockRollbackMap){
        if(stockRollbackMap.isEmpty()){
            return;
        }
        log.warn("开始回滚库存：{}", stockRollbackMap);
        for(Map.Entry<Long, Integer> entry : stockRollbackMap.entrySet()){
            Long skuId = entry.getKey();
            Integer quantityToRollback = entry.getValue();
            if(skuId != null && quantityToRollback != null && quantityToRollback > 0){
                LambdaUpdateWrapper<ProductSku> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ProductSku::getId, skuId)
                        .setSql("stock = stock +" + quantityToRollback);
                int affectedRows = productSkuMapper.update(updateWrapper);
                if(affectedRows <= 0){
                    //极端情况，回滚也失败了，记录严重错误日志
                    log.error("！！！严重：回滚库存失败！SKU ID：{}，数量：{}",skuId, quantityToRollback);
                }else{
                    log.info("SKU ID：{} 库存回滚蔡成功：{}", skuId,quantityToRollback);
                }
            }
        }
    }

    /**
     * 生成唯一订单号（父订单子订单通用）
     * @param prefix 订单号前缀(例如 "P" for Parent, "S" for Sub)
     * @return 唯一的订单号字符串
     */
    private String generateOrderNumber(String prefix){
        // 格式：前缀 + yyyyMMddHHmmssSSS + 4位随机数
        String timeStamp = LocalDateTime.now().format(ORDER_NUMBER_FORMATTER);
        String randomDigits = String.format("%04d", ThreadLocalRandom.current().nextInt(10000)); //0000-9999
        return prefix + timeStamp + randomDigits;
    }

    /**
     * 创建SKU快照JSON对象
     * @param sku ProductSku实体
     * @param product Product实体（用于获取SPU名称等）
     * @return Map<String, Object>用于Jackson序列化
     */
    private Map<String, Object> createSkuSnapshot(ProductSku sku, Product product){
        Map<String, Object> snapShot = new HashMap<>();
        snapShot.put("skuId", sku.getId());
        snapShot.put("productId", product.getId());
        snapShot.put("productName", product.getName());
        snapShot.put("specifications", sku.getSpecifications());
        snapShot.put("image", sku.getImage() != null ? sku.getImage() : product.getMainImage()); //优先用SKU图片
        //可以根据需要添加更多字段

        return snapShot;
    }

    //辅助内部类，用于临时存储订单项相关信息
    @Data
    private static class OrderItemDetail{
        private Long storeId;
        private Long productId;
        private Long skuId;
        private Integer quantity;
        private BigDecimal priceSnapshot;
        private ProductSku sku; //用于创建快照
        private Product product; //同上

        public OrderItemDetail(Long storeId, Long productId, Long skuId, Integer quantity, BigDecimal priceSnapshot, ProductSku sku, Product product) {
            this.storeId = storeId;
            this.productId = productId;
            this.skuId = skuId;
            this.quantity = quantity;
            this.priceSnapshot = priceSnapshot;
            this.sku = sku;
            this.product = product;
        }
    }


    //====内部方法结束====

    /**
     * 创建订单（FR-OM-001）
     */
    @Override
    @Transactional
    public OrderSubmitVO createOrder(Long userId, CreateOrderDTO createOrderDTO){

        List<Long> cartItemIds = createOrderDTO.getCartItemIds();
        Long addressId = createOrderDTO.getAddressId();

        //1.数据校验
        //1.1校验并获取购物车项
        List<ShoppingCart> cartItems = cartService.listByIds(cartItemIds);
        if(CollectionUtils.isEmpty(cartItems) || cartItems.size() != cartItemIds.size()){
            throw new BusinessException(400,"包含无效的购物车项ID");
        }
        //验证购物车项所有权
        for(ShoppingCart item : cartItems){
            if(!Objects.equals(item.getUserId(), userId)){
                log.warn("用户 {} 尝试结算不属于自己的购物车项 {}", userId, item.getId());
                throw new BusinessException(403,"无权操作部分购物车商品");
            }
        }

        //1.2校验并获取收货地址，生成快照
        UserAddress address = addressService.getById(addressId);
        if(address == null || !Objects.equals(address.getUserId(), userId)){
            throw new BusinessException(403,"收货地址无效或无权使用");
        }
        Map<String, Object> addressSnapshot = objectMapper.convertValue(address, new TypeReference<>() {});

        //1.3获取待结算SKU的详细信息（包括storeId，productId，price，stock）并检查状态
        List<Long> skuIds = cartItems.stream().map(ShoppingCart::getSkuId).distinct().toList();
        Map<Long, ProductSku> skuMap = productSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, p -> p));
        //同时获取对应SPU的信息（为了获取storeId）
        List<Long> productIds = skuMap.values().stream().map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        //准备好存储回滚信息的数据结构
        Map<Long, Integer> stockRollbackMap = new HashMap<>();
        //准备好每个购物车项对应的详细信息（包括快照所需）
        Map<Long, OrderItemDetail> itemDetailMap = new HashMap<>();

        BigDecimal parentTotalAmount = BigDecimal.ZERO;

        //开始逐个处理订单商品
        for(ShoppingCart cartItem : cartItems){
            ProductSku sku = skuMap.get(cartItem.getSkuId());
            if(sku == null || sku.getStatus() == 0){
                //如果在获取详情时SKU已失效，则无法下单
                throw new BusinessException(400, "商品SKU ID：" + cartItem.getSkuId() + "已失效");
            }
            Product product = productMap.get(sku.getProductId());
            if(product == null || product.getStatus() == 0){
                //同理，如果SPU已失效，则无法下单
                throw new BusinessException(400, "商品SPU ID：" + sku.getProductId() + "已失效");
            }

            //2.库存预扣
            int quantityToDeduct = cartItem.getQuantity();;
            boolean success = deductStock(sku.getId(), quantityToDeduct);

            if(!success){
                //2.1预扣失败，执行库存回滚并抛出异常
                log.warn("SKU ID: {} 库存不足，需要扣减: {}，实际: 不足", sku.getId(), quantityToDeduct);
                rollBackStock(stockRollbackMap);
                throw new BusinessException(400, "商品 [" + product.getName() + "] 库存不足，下单失败");
            }else{
                //记录成功扣减的库存，以便失败时回滚
                stockRollbackMap.put(sku.getId(), quantityToDeduct);
                log.info("SKU ID: {} 预扣库存成功: {}", sku.getId(), quantityToDeduct);
            }

            //计算父订单总金额
            BigDecimal currentItemTotal = sku.getPrice().multiply(BigDecimal.valueOf(quantityToDeduct));
            parentTotalAmount = parentTotalAmount.add(currentItemTotal);

            //存储订单项所需信息
            itemDetailMap.put(cartItem.getId(), new OrderItemDetail(
                    product.getStoreId(),
                    product.getId(),
                    sku.getId(),
                    quantityToDeduct,
                    sku.getPrice(), // 价格快照
                    sku, // SKU 完整信息，用于快照
                    product // SPU 信息，用于快照
            ));
        }

        //3.订单拆分与创建
        //3.1按店铺分组
        Map<Long, List<ShoppingCart>> itemsByStore = cartItems.stream()
                .collect(Collectors.groupingBy(item -> itemDetailMap.get(item.getId()).getStoreId()));

        //3.2创建父订单
        ParentOrder parentOrder = new ParentOrder();
        parentOrder.setParentOrderNumber(generateOrderNumber("p"));
        parentOrder.setUserId(userId);
        parentOrder.setTotalAmount(parentTotalAmount);
        // paymentAmount, paymentMethod, paymentTime 等支付成功后更新
        parentOrder.setCreateTime(LocalDateTime.now());
        parentOrder.setUpdateTime(LocalDateTime.now());
        parentOrderMapper.insert(parentOrder);

        Long parentOrderId = parentOrder.getId(); //获取父订单id

        List<String> subOrderNumbers = new ArrayList<>(); //收集子订单号

        //3.3创建子订单和订单项
        for(Map.Entry<Long,List<ShoppingCart>> entry : itemsByStore.entrySet()){
            Long storeId = entry.getKey();
            List<ShoppingCart> storeCartItems = entry.getValue();

            //计算该店铺子订单总金额
            BigDecimal subTotalAmount = storeCartItems.stream()
                    .map(item -> {
                        OrderItemDetail detail = itemDetailMap.get(item.getId());
                        return detail.getPriceSnapshot().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            //创建子订单
            Order subOrder = new Order();
            String subOrderNumber = generateOrderNumber("S"); //生成子订单号
            subOrderNumbers.add(subOrderNumber); //收集子订单号

            subOrder.setParentOrderId(parentOrderId);
            subOrder.setStoreId(storeId);
            subOrder.setOrderNumber(subOrderNumber);
            subOrder.setUserId(userId);
            subOrder.setTotalAmount(subTotalAmount);
            subOrder.setStatus(0); //待支付
            subOrder.setAddressSnapshot(addressSnapshot);
            subOrder.setNote(createOrderDTO.getNote());
            subOrder.setCreateTime(LocalDateTime.now());
            subOrder.setUpdateTime(LocalDateTime.now());
            subOrder.setVersion(0); //初始版本号为0
            // shippingInfo, shippingTime, completionTime, cancelTime, previousStatus 在后续流程更新
            this.save(subOrder);
            Long subOrderId = subOrder.getId();

            //创建订单明细
            List<OrderItem> orderItems = new ArrayList<>();
            for(ShoppingCart cartItem : storeCartItems){
                OrderItemDetail detail = itemDetailMap.get(cartItem.getId());
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(subOrderId);
                orderItem.setStoreId(storeId);
                orderItem.setProductId(detail.getProductId());
                orderItem.setSkuId(detail.getSkuId());
                orderItem.setQuantity(detail.getQuantity());
                orderItem.setPriceSnapshot(detail.getPriceSnapshot());

                //创建SKU快照
                Map<String, Object> skuSnapshot = createSkuSnapshot(detail.getSku(), detail.getProduct());
                orderItem.setSkuSnapshot(skuSnapshot);

                orderItem.setReviewStatus(0); //待评价
                orderItems.add(orderItem);
            }
            //批量插入订单明细
            if(!orderItems.isEmpty()){
                // 使用 MyBatis Plus 的 saveBatch (需要 OrderItemService 注入或直接用 Mapper)
                // 这里直接用 Mapper
                orderItems.forEach(orderItemMapper::insert);
                //TODO:考虑更优化的批量插入方式
            }

        }

        //4.后处理
        //4.1从购物车移除已下单的商品
        cartService.removeByIds(cartItems);
        //同时删除购物车缓存
        String cartCacheKey = RedisKeyUtil.getCartUserKey(userId);
        stringRedisTemplate.delete(cartCacheKey);
        log.info("订单创建成功，删除购物车项: {}, 并删除缓存: {}", cartItemIds, cartCacheKey);

        //4.2发送延迟消息
        //TODO:需要引入RabbitMQ依赖和配置
        log.info("TODO: 发送订单 {} 的延迟取消消息到 RabbitMQ", parentOrder.getParentOrderNumber());

        //5.返回结果
        OrderSubmitVO submitVO = new OrderSubmitVO();
        submitVO.setParentOrderNumber(parentOrder.getParentOrderNumber());
        submitVO.setTotalAmount(parentTotalAmount);
        // submitVO.setSubOrderNumbers(subOrderNumbers); // 根据需要返回子订单号列表

        return submitVO;
    }

    /**
     *分页查询用户订单列表
     */
    @Override
    public PageResult<OrderListVO> getOrderList(Long userId, OrderListQueryDTO queryDTO){
        //1.构建分页对象
        Page<Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        //2.构建查询条件(查询子订单表orders）
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);

        //3.按状态筛选
        if(queryDTO.getStatus() != null){
            queryWrapper.eq(Order::getStatus, queryDTO.getStatus());
        }

        //按创建时间倒序排列
        queryWrapper.orderByDesc(Order::getCreateTime);

        //3.执行分页查询
        Page<Order> orderPage = this.page(page, queryWrapper);
        List<Order> orders = orderPage.getRecords();

        // 如果当前页没有数据，直接返回空结果
        if (orders.isEmpty()) {
            return new PageResult<>(orderPage.getTotal(), Collections.emptyList());
        }

        //4.提取子订单ID列表和店铺ID列表，用于批量查询
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<Long> storeIds = orders.stream().map(Order::getStoreId).toList();

        //5.批量查询订单明细（order_item)
        Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
        if(!orderIds.isEmpty()){
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(OrderItem::getOrderId, orderIds);
            List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);
            //按orderId分组
            orderItemsMap = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        }

        //6.批量查询店铺信息
        Map<Long, Store> storeMap = new HashMap<>();
        if(!storeIds.isEmpty()){
            List<Store> stores = storeService.listByIds(storeIds);
            storeMap = stores.stream().collect(Collectors.toMap(Store::getId, Function.identity()));
        }

        //7.组装OrderListVO列表
        final Map<Long, List<OrderItem>> finalOrderItemsMap = orderItemsMap; // lambda 需要 final
        final Map<Long, Store> finalStoreMap = storeMap; // lambda 需要 final

        List<OrderListVO> voList = orders.stream().map(order -> {
            OrderListVO vo = new OrderListVO();
            BeanUtils.copyProperties(order, vo);  // 拷贝 orderId, orderNumber, totalAmount, status, createTime
            vo.setOrderId(order.getId()); //确保id被拷贝

        //设置店铺名称
            Store store = finalStoreMap.get(order.getStoreId());
            vo.setStoreName(store != null ? store.getName() : "未知店铺");

            //获取该订单的明细
            List<OrderItem> items = finalOrderItemsMap.getOrDefault(order.getId(), Collections.emptyList());

            //转换订单明细为OrderItemInListVO
            List<OrderItemInListVO> itemVOs = items.stream().map(item -> {
                OrderItemInListVO itemVo = new OrderItemInListVO();
                itemVo.setSkuId(item.getSkuId());
                itemVo.setQuantity(item.getQuantity());
                itemVo.setPrice(item.getPriceSnapshot());

                //从skuSnapshot（JSON）中解析出商品名、规格、图片
                Map<String, Object> skuSnapshot = item.getSkuSnapshot();
                if(skuSnapshot != null){
                    itemVo.setProductName((String) skuSnapshot.getOrDefault("productName", "商品信息缺失"));

                    //解析规格：skuSnapshot里的specifications是List<Map<String, String>>
                    try{
                        Object specObj = skuSnapshot.get("specifications");
                        if(specObj instanceof List){
                            //使用ObjectMapper转换，更健壮
                            List<Map<String, String>> specs = objectMapper.convertValue(specObj, new TypeReference<List<Map<String, String>>>() {});
                            String specDesc = specs.stream()
                                    .map(spec -> spec.get("key") + ":" + spec.get("value"))
                                    .collect(Collectors.joining("; "));
                            itemVo.setSpecifications(specDesc);
                        }else{
                            itemVo.setSpecifications("规格信息错误");
                        }
                    }catch (Exception e){
                        log.error("解析SKU快照规格失败，item id：{}", item.getId());
                        itemVo.setSpecifications("规格解析异常");
                    }
                    itemVo.setImage((String) skuSnapshot.get("image"));
                }else{
                    // 处理快照为空的情况
                    itemVo.setProductName("商品快照丢失");
                    itemVo.setSpecifications("");
                    itemVo.setImage(""); // 后续可以设置一个默认图片URL
                }
                return itemVo;
            }).toList();

            vo.setItems(itemVOs);

            return vo;
        }).toList();

        //8.封装并返回PageResult
        return new PageResult<>(orderPage.getTotal(), voList);
    }

    /**
     * 查询订单详情（FR-OM-003）
     */
    @Override
    public OrderDetailVO getOrderDetail(Long userId, String orderNumber){

        //1.根据OrderNumber查询子订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        //2.校验订单是否存在以及归属权
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if(!Objects.equals(order.getUserId(), userId)){
            log.warn("用户 {} 尝试访问不属于自己的订单 {}", userId, orderNumber);
            throw new BusinessException(404,"订单不存在"); //对外统一口径
        }
        // 3.查询关联的订单明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        //4.为了获取支付时间和父订单号，需要查询关联的父订单
        ParentOrder parentOrder = parentOrderMapper.selectById(order.getParentOrderId());
        String parentOrderNumber = (parentOrder != null) ? parentOrder.getParentOrderNumber() : null;
        LocalDateTime paymentTime = (parentOrder != null) ? parentOrder.getPaymentTime() : null;

        //5.查询店铺名称
        Store store = storeService.getById(order.getStoreId());
        String storeName = (store != null) ? store.getName() : "未知店铺";

        //6.组装OrderDetailVO
        OrderDetailVO detailVO = new OrderDetailVO();
        BeanUtils.copyProperties(order, detailVO);
        detailVO.setOrderId(order.getId());
        detailVO.setStoreName(storeName);
        detailVO.setParentOrderNumber(parentOrderNumber);
        detailVO.setPaymentTime(paymentTime);

        //填充OrderItemDetailVO列表
        List<OrderItemDetailVO> itemDetailVOS = items.stream().map(item -> {
            OrderItemDetailVO itemVO = new OrderItemDetailVO();
            BeanUtils.copyProperties(item, itemVO); // 拷贝基础属性 (skuId, quantity, priceSnapshot, reviewStatus)
            itemVO.setOrderItemId(item.getId());

            //解析快照
            Map<String, Object> skuSnapshot = item.getSkuSnapshot();
            if(skuSnapshot != null){
                itemVO.setProductName((String) skuSnapshot.getOrDefault("productName", "商品信息缺失"));
                itemVO.setImage((String) skuSnapshot.get("image"));

                //解析规格
                try{
                    Object specObj = skuSnapshot.get("specifications");
                    if(specObj instanceof List){
                        List<Map<String, String>> specs = objectMapper.convertValue(specObj, new TypeReference<List<Map<String, String>>>() {});
                        String specDesc = specs.stream()
                                .map(spec -> spec.get("key") + ":" + spec.get("value"))
                                .collect(Collectors.joining("; "));
                        itemVO.setSpecifications(specDesc);
                    }else{itemVO.setSpecifications("规格信息错误");}
                }catch (Exception e){
                    log.error("解析SKU快照规格失败, item id: {}", item.getId(), e);
                    itemVO.setSpecifications("规格解析异常");
                }
            }else{
                itemVO.setProductName("商品快照丢失");
                itemVO.setSpecifications("");
                itemVO.setImage("");
            }
            return itemVO;
        }).toList();

        detailVO.setItems(itemDetailVOS);

        return detailVO;
    }


    /**
     * 用户取消订单 (FR-OM-005)
     */
    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNumber){
        //1.根据OrderNumber查询子订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        //2.校验订单是否存在以及归属权
        if(order == null){
            throw new BusinessException(404,"订单不存在");
        }
        if(!Objects.equals(order.getUserId(), userId)){
            log.warn("用户 {} 尝试取消不属于自己的订单 {}", userId, orderNumber);
            throw new BusinessException(404, "订单不存在"); // 对外统一口径
        }

        //3.状态校验
        Integer currentStatus = order.getStatus();
        // 允许取消的状态： 0 (待支付) 或 1 (待发货)
        if(currentStatus != 0 && currentStatus != 1){
            throw new BusinessException(409,"订单状态异常，无法取消");
        }

        //4.如果是待发货状态，需要先发起退款
        if(currentStatus == 1){
            //TODO:实现支付模块，调用其退款接口
            log.info("TODO: 调用支付服务为订单 {} 发起退款...", orderNumber);
            // boolean refundAccepted = paymentService.requestRefund(orderNumber);
            // if (!refundAccepted) {
            //     throw new BusinessException(500, "发起退款失败，请稍后重试或联系客服");
            // }
        }

        //5.更新订单状态为“已退款”
        Order orderToUpdate = new Order();
        orderToUpdate.setId(order.getId());
        orderToUpdate.setStatus(4); //已取消
        orderToUpdate.setCancelTime(LocalDateTime.now());
        orderToUpdate.setUpdateTime(LocalDateTime.now());
        // 这里没有用乐观锁，因为用户取消操作相对低频，且状态校验已前置。
        // 如果未来需要更强的并发控制，可以加入 version 字段的更新。
        boolean updated = this.updateById(orderToUpdate);
        if(!updated){
            //// 更新失败，可能订单已被删除或其他并发问题
            log.error("更新订单 {} 状态为已取消失败！", orderNumber);
            throw new BusinessException(500, "取消订单失败，请稍后重试");
        }

        //6.释放库存（回滚库存）
        //6.1查询订单明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        //6.2构建回滚map
        Map<Long, Integer> stockToRollback = items.stream()
                .collect(Collectors.toMap(OrderItem::getSkuId, OrderItem::getQuantity, Integer::sum)); //// 使用 sum 以防万一有重复 SKU (理论上不应发生)

        //6.3执行回滚
        rollBackStock(stockToRollback);

        log.info("用户 {} 取消订单 {} 成功，已释放库存。", userId, orderNumber);
    }

    /**
     * 商家发货（FR-OM-006）
     */
    @Override
    @Transactional
    public void shipOrder(Long merchantUserId, String orderNumber, ShipOrderDTO shipOrderDTO){

        //1.根据orderNumber查询子订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        if(order == null){
            throw new BusinessException(404, "订单不存在");
        }

        //2.校验权限
        //2.1校验店铺归属
        Store store = storeService.getById(order.getStoreId());
        if(store == null || !Objects.equals(store.getUserId(), merchantUserId)){
            log.warn("商家 {} 尝试操作不属于自己的店铺 {} 的订单 {}", merchantUserId, order.getStoreId(), order.getId());
            throw new BusinessException(403, "无权操作该订单");
        }

        //2.2校验订单状态
        if(!Objects.equals(order.getStatus(), 1)){ //必须是1（待发货）
            log.warn("商家 {} 尝试对状态为 {} 的订单 {} 发货", merchantUserId, order.getStatus(), orderNumber);
            throw new BusinessException(409, "订单状态不正确，无法发货");
        }

        //3.校验通过，更新订单状态
        Order orderToUpdate = new Order();
        orderToUpdate.setId(order.getId());
        orderToUpdate.setStatus(2); //2：已发货
        orderToUpdate.setShippingTime(LocalDateTime.now());

        //组装物流信息JSON
        Map<String, Object> shippingInfo = new HashMap<>();
        shippingInfo.put("shippingCompany", shipOrderDTO.getShippingCompany());
        shippingInfo.put("trackingNumber", shipOrderDTO.getTrackingNumber());
        orderToUpdate.setShippingInfo(shippingInfo);

        orderToUpdate.setUpdateTime(LocalDateTime.now());

        // 使用乐观锁 (如果需要)
        // orderToUpdate.setVersion(order.getVersion()); // 传入当前版本号
        // queryWrapper.eq(Order::getVersion, order.getVersion()); // 确保更新时版本号一致
        // boolean updated = this.update(orderToUpdate, queryWrapper);

        //暂不使用乐观锁
        boolean updated = this.updateById(orderToUpdate);

        if(!updated){
            log.error("商家发货更新订单 {} 状态失败！", orderNumber);
            throw new BusinessException(500,"发货失败，请稍后再试");
        }

        //TODO:触发对用户的通知
        //log.info("订单 {} 已发货，触发通知用户...", orderNumber);
    }

    /**
     * 用户确认收货 (FR-OM-010)
     */
    @Override
    @Transactional
    public void userCompleteOrder(Long userId, String orderNumber){
        //1.根据orderNumber查询子订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        //2.校验订单是否存在以及归属权
        if (order == null) {
            throw new BusinessException(404,"订单不存在");
        }
        if(!Objects.equals(order.getUserId(), userId)){
            log.warn("用户 {} 尝试确认收货不属于自己的订单 {}", userId, orderNumber);
            throw new BusinessException(404,"订单不存在");
        }

        //3.状态校验
        if(!Objects.equals(order.getStatus(), 2)){ //必须是2（已发货）
            log.warn("用户 {} 尝试确认状态为 {} 的订单 {}", userId, order.getStatus(), orderNumber);
            throw new BusinessException(409,"订单状态不正确，无法确认收货");
        }

        //4.更新订单状态为“已完成”
        Order orderToUpdate = new Order();
        orderToUpdate.setId(order.getId());
        orderToUpdate.setStatus(3); //3：已完成
        orderToUpdate.setCompletionTime(LocalDateTime.now()); //记录完成时间
        orderToUpdate.setUpdateTime(LocalDateTime.now());

        // (可选) 使用乐观锁
        // orderToUpdate.setVersion(order.getVersion());
        // LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        // updateWrapper.eq(Order::getId, order.getId())
        //              .eq(Order::getVersion, order.getVersion());
        // boolean updated = this.update(orderToUpdate, updateWrapper);

        boolean updated = this.updateById(orderToUpdate);

        if(!updated){
            log.error("用户确认收货后，更新订单 {} 状态失败！", orderNumber);
            throw new BusinessException(500,"确认收货失败，请稍后重试");
        }

        //5.发送异步消息更新销量
        //5.1查询订单明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId())
                .select(OrderItem::getProductId, OrderItem::getSkuId, OrderItem::getQuantity); //只需要这几个字段

        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        //5.2 TODO:发送消息到RabbitMQ
        log.info("TODO: 订单 {} 已完成，发送异步消息更新商品销量: {}", orderNumber, items);
        //rabbitTemplate.convertAndSend("exchange.sales.update", "", items);
    }

    /**
     * 商家查询店铺订单列表 (FR-OM-009)
     */
    @Override
    public PageResult<OrderListVO> getMerchantOrderList(Long merchantUserId, Long storeId, OrderListQueryDTO queryDTO){
        //1.权限校验，检查当前商家是否拥有该店铺
        Store store = storeService.getById(storeId);
        if(store == null || !Objects.equals(store.getUserId(), merchantUserId)){
            log.warn("商家 {} 尝试查询不属于自己的店铺 {} 的订单", merchantUserId, storeId);
            throw new BusinessException(403, "无权操作该店铺");
        }

        //2.构造分页对象
        Page<Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        //3.构造查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStoreId, storeId); //按店铺id查询

        //按状态筛选
        if(queryDTO.getStatus() != null){
            queryWrapper.eq(Order::getStatus, queryDTO.getStatus());
        }

        //按创建时间倒序排列（商家可能更关心新订单）
        queryWrapper.orderByDesc(Order::getCreateTime);

        //4.执行分页查询
        Page<Order> orderPage = this.page(page, queryWrapper);
        List<Order> orders = orderPage.getRecords();

        if(orders.isEmpty()){
            return new PageResult<>(orderPage.getTotal(), Collections.emptyList());
        }

        //5.批量查询订单明细
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
        if(!orderIds.isEmpty()){
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(OrderItem::getOrderId, orderIds);
            List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);
            orderItemsMap = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        }

        final Map<Long, List<OrderItem>> finalOrderItemsMap = orderItemsMap; //lambda需要final

        //6.组装OrderListVO列表（复用getOrderList逻辑）
        List<OrderListVO> voList = orders.stream().map(order -> {
            OrderListVO vo = new OrderListVO();
            BeanUtils.copyProperties(order, vo);
            vo.setOrderId(order.getId());
            vo.setStoreName(store.getName()); // 直接使用已查询到的店铺名称

            List<OrderItem> items = finalOrderItemsMap.getOrDefault(order.getId(), Collections.emptyList());

            List<OrderItemInListVO> itemVOs = items.stream().map(item -> {
                OrderItemInListVO itemVO = new OrderItemInListVO();
                itemVO.setSkuId(item.getSkuId());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setPrice(item.getPriceSnapshot());

                Map<String, Object> skuSnapshot = item.getSkuSnapshot();
                if(skuSnapshot != null){
                    itemVO.setProductName( (String) skuSnapshot.getOrDefault("productName", "商品信息缺失"));

                    try{
                        Object specObj = skuSnapshot.get("specifications");
                        if(specObj instanceof List){
                            List<Map<String, String>> specs = objectMapper.convertValue(specObj, new TypeReference<List<Map<String, String>>>() {});
                            String specDesc = specs.stream()
                                    .map(spec -> spec.get("key") + ":" + spec.get("value"))
                                    .collect(Collectors.joining("; "));
                            itemVO.setSpecifications(specDesc);
                        }else{itemVO.setSpecifications("规格信息错误");}
                    }catch (Exception e){
                        log.error("解析SKU快照规格失败, item id: {}", item.getId(), e);
                        itemVO.setSpecifications("规格解析异常");
                    }
                    itemVO.setImage((String) skuSnapshot.get("image"));
                }else{
                    itemVO.setProductName("商品快照丢失");
                    itemVO.setSpecifications("");
                    itemVO.setImage("");
                }
                return itemVO;
            }).toList();

            vo.setItems(itemVOs);
            return vo;
        }).toList();

        return new PageResult<>(orderPage.getTotal(), voList);
    }

    /**
     * 用户申请退款（FR-OM-007）
     */
    /*@Override
    @Transactional
    public void applyForRefund(Long userId, String orderNumber, RefundApplicationDTO refundDTO){

        //1.根据orderNumber查询订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        //2.校验订单是否存在及归属
        if(order == null){
            throw new BusinessException(404,"订单不存在");
        }
        if(!Objects.equals(order.getUserId(), userId)){
            log.warn("用户 {} 尝试为不属于自己的订单 {} 申请退款", userId, orderNumber);
            throw new BusinessException(404,"订单不存在");
        }

        //3.状态校验
        Integer currentStatus = order.getStatus();
        // 允许申请退款的状态： 1 (待发货) 或 2 (已发货)
        if(currentStatus != 1 && currentStatus != 2){
            log.warn("用户 {} 尝试为状态为 {} 的订单 {} 申请退款", userId, currentStatus, orderNumber);
            throw new BusinessException(409,"订单当前状态无法申请退款");
        }

        //4.更新订单状态
        Order orderToUpdate = new Order();
        orderToUpdate.setId(order.getId());
        orderToUpdate.setStatus(5); //退款中
        orderToUpdate.setPreviousStatus(currentStatus); //记录原状态（1或2）
        orderToUpdate.setUpdateTime(LocalDateTime.now());

        // (可选) 记录退款原因到 note 字段（数据库还没有这个字段）
        // String newNote = (order.getNote() != null ? order.getNote() : "") + " [退款原因: " + refundDTO.getReason() + "]";
        // orderToUpdate.setNote(newNote);

        boolean updated = this.updateById(orderToUpdate);

        if(!updated){
            log.error("更新订单 {} 状态为“退款中”失败！", orderNumber);
            throw new BusinessException(500,"申请退款失败，请稍后重试");
        }

        // 5. TODO: 在订单模块中创建退款流水 (来源 619)
        log.info("TODO: 订单 {} 已申请退款，原因: {}，需创建退款流水记录", orderNumber, refundDTO.getReason());

        log.info("用户 {} 为订单 {} 申请退款成功。", userId, orderNumber);
    }*/

    /**
     * 商家审核退款申请（FR-OM-008）
     */
    /*@Override
    @Transactional
    public void approveRefund(Long merchantUserId, String orderNumber, RefundApproveDTO approveDTO){
        //1.根据orderNumber查询订单信息
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getOrderNumber, orderNumber);
        Order order = this.getOne(queryWrapper);

        if(order == null){
            throw new BusinessException(404,"订单不存在");
        }

        //2.权限校验（商家是否有该订单对应的店铺）
        Store store = storeService.getById(order.getStoreId());
        if(!Objects.equals(store.getUserId(), merchantUserId)){
            log.warn("商家 {} 尝试审核不属于自己店铺的订单 {} 的退款申请", merchantUserId, orderNumber);
            throw new BusinessException(403, "无权操作该订单");
        }

        //3.校验订单状态（必须是“退款中”）
        if(!Objects.equals(order.getStatus(), 5)){ //5: 退款中
            log.warn("商家 {} 尝试审核状态为 {} 的订单 {}", merchantUserId, order.getStatus(), orderNumber);
            throw new BusinessException(409, "订单状态不正确，无法审核");
        }

        Order orderToUpdate = new Order();
        orderToUpdate.setId(order.getId());
        orderToUpdate.setUpdateTime(LocalDateTime.now());

        if(RefundActionType.APPROVE.equals(approveDTO.getAction())){
            //4.同意退款

            //4.1 TODO:调用支付模块发起退款
            log.info("TODO：订单 {} 退款申请已同意，调用支付服务执行退款...", orderNumber);
            // boolean refundSuccess = paymentService.executeRefund(orderNumber);
            // if (!refundSuccess) {
            //     throw new BusinessException(500, "退款失败，请联系支付服务");
            // }

            //4.2 更新订单状态
            orderToUpdate.setStatus(6); //6: 已退款
            orderToUpdate.setPreviousStatus(null); //清空该字段（文档设计要求）

            //4.3 TODO：处理库存回补（不一定在这里进行操作，因为一般来说需要商家确认收到退货商品才回补）
            // (退款是否补回库存，取决于业务策略。例如：
            // 如果是“待发货”(1)时退款，库存应该回补。
            // 如果是“已发货”(2)时退款(退货退款)，库存是否回补取决于商品是否已入库。
            // 此处暂不处理库存，仅记录日志)
            if(Objects.equals(order.getPreviousStatus(), 1)){
                log.info("TODO: 订单 {} (原状态:待发货) 退款成功，应回滚库存。", orderNumber);
                //调用rollBackStock方法
            }else if(Objects.equals(order.getPreviousStatus(), 2)){
                log.info("TODO: 订单 {} (原状态:已发货) 退款成功，需根据商品是否退回决定是否回滚库存。", orderNumber);
            }
        }else if(RefundActionType.REJECT.equals(approveDTO.getAction())){
            //5.拒绝退款
            Integer previousStatus = order.getPreviousStatus();
            if(previousStatus == null){
                // 状态 5 必定应该有 previousStatus，如果没有，说明数据异常
                log.error("数据异常：订单 {} 处于退款中，但 previous_status 为空！", orderNumber);
                throw new BusinessException(500,"订单数据异常，操作失败");
            }

            //恢复订单状态
            orderToUpdate.setStatus(previousStatus);
            orderToUpdate.setPreviousStatus(null); //清空标记

            //记录拒绝理由
            String reason = (approveDTO.getReason() != null && !approveDTO.getReason().isEmpty())
                    ? approveDTO.getReason() : "商家未填写拒绝原因";
            String newNote = (order.getNote() != null ? order.getNote() : "") + " [退款被拒: " + reason + "]";
            orderToUpdate.setNote(newNote); // TODO：暂存到 note（订单备注）字段，实际需要新建一个拒绝退款原因字段。
        }else{
            throw new BusinessException(400, "无效的操作类型");
        }

        //6.执行数据库更新
        boolean updated = this.updateById(orderToUpdate);
        if(!updated){
            log.error("商家审核退款，更新订单 {} 状态失败！", orderNumber);
            throw new BusinessException(500, "操作失败，请稍后重试！");
        }

        //TODO: 通知用户退款申请被拒
        log.info("TODO: 订单 {} 退款申请已处理 (Action: {})，通知用户。", orderNumber, approveDTO.getAction());

    }*/

}
