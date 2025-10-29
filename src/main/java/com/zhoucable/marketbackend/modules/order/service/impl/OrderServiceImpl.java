package com.zhoucable.marketbackend.modules.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import com.zhoucable.marketbackend.modules.address.service.AddressService;
import com.zhoucable.marketbackend.modules.order.dto.CreateOrderDTO;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.entity.OrderItem;
import com.zhoucable.marketbackend.modules.order.entity.ParentOrder;
import com.zhoucable.marketbackend.modules.order.mapper.OrderItemMapper;
import com.zhoucable.marketbackend.modules.order.mapper.OrderMapper;
import com.zhoucable.marketbackend.modules.order.mapper.ParentOrderMapper;
import com.zhoucable.marketbackend.modules.order.service.OrderService;
import com.zhoucable.marketbackend.modules.order.vo.OrderSubmitVO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
     * 回滚库存（用于下单失败时）
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
        Long addressId = createOrderDTO.getAddressI();

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

}
