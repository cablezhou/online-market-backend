package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.product.dto.*;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.mapper.ProductMapper;
import com.zhoucable.marketbackend.modules.product.mapper.ProductSkuMapper;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import com.zhoucable.marketbackend.modules.product.service.ProductSkuService;
import com.zhoucable.marketbackend.utils.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private StoreService storeService; //用于校验店铺归属

    @Autowired
    private ProductSkuService productSkuService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate; //用于分布式锁

    @Autowired
    private ObjectMapper objectMapper;

    //Redis缓存Key前缀和过期时间
    private static final String CACHE_PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final long CACHE_PRODUCT_DETAIL_TTL_HOURS = 24; // 基础 TTL


    /**
     * 私有方法，更新SPU的展示价格
     * 查询商品下所有在售SKU的最低价格，并更新到product表的display_price字段
     * @param productId 商品SPU id
     */
    private void updateProductDisplayPrice(Long productId){
        if(productId == null){
            return;
        }

        //1.查询该SPU下所有在售的SKU的最低价格
        BigDecimal minPrice = findMinSkuPrice(productId);

        //2.更新product表
        Product productUpdate = new Product();
        productUpdate.setId(productId);
        productUpdate.setDisplayPrice(minPrice);
        //不更新updateTime，因为这只是一个内部冗余字段的更新
        boolean success = this.updateById(productUpdate);

        if(!success){
            //更新失败，可能商品刚被删除，记录日志
            log.warn("更新商品 SPU {} 的 display_price 失败，可能已被删除。", productId);
            //throw new BusinessException(4013, "更新商品展示价格失败"); //在考虑要不要抛出这个异常
        }else{
            log.info("已更新商品 SPU {} 的 display_price 为: {}", productId, minPrice);
        }

    }

    @Override
    @Transactional
    public Product createProduct(ProductCreateDTO createDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(4012, "请先登录");
        }

        //1.校验店铺归属
        Store store = storeService.getById(createDTO.getStoreId());
        if (store == null || !store.getUserId().equals(userId)){
            throw new BusinessException(4031, "无权操作该店铺");
        }

        //2.创建Product（SPU）对象并插入数据库
        Product product = new Product();
        product.setStoreId(createDTO.getStoreId());
        product.setName(createDTO.getName());
        product.setCategory1Id(createDTO.getCategory1Id());
        product.setCategory2Id(createDTO.getCategory2Id());
        product.setCategory3Id(createDTO.getCategory3Id());
        product.setMainImage(createDTO.getMainImage());
        product.setImages(createDTO.getImages());
        product.setDetailContent(createDTO.getDetailContent());
        product.setStatus(1); //默认上架
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        this.save(product); //保存后会获得id

        Long productId = product.getId();

        //3.处理SKU列表
        List<ProductSku> savedSkus = new ArrayList<>();
        String lockKey = "lock:sku:create:product" + productId; //分布式锁 Key
        String lockValue = UUID.randomUUID().toString(); //锁的唯一值

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        try{
            //尝试获取分布式锁（最多等待1秒，锁持有3秒）
            Boolean acquired = ops.setIfAbsent(lockKey, lockValue, Duration.ofSeconds(3));
            if(Boolean.FALSE.equals(acquired)){
                throw new BusinessException(5031, "系统繁忙，请稍后重试"); //获取锁失败
            }

            //---进入临界区---
            for (SkuDTO skuDTO : createDTO.getSkus()) {
                // a. --- 规格标准化 ---
                // a.1 先对 DTO 列表排序
                List<SpecificationDTO> sortedSpecDTOs = skuDTO.getSpecifications().stream()
                        .sorted(Comparator.comparing(SpecificationDTO::getKey))
                        .collect(Collectors.toList());

                // a.2 将排序后的 DTO 列表转换为 JSON 字符串 (用于查重)
                String standardizedSpecJson = productSkuService.standardizeSpecifications(sortedSpecDTOs); // 调用 standardize 方法

                // c. 检查规格是否重复 (在锁内检查)
                if (productSkuService.checkDuplicateSku(productId, standardizedSpecJson)) {
                    throw new BusinessException(4092, "SKU 规格重复：" + standardizedSpecJson);
                }

                // e. 创建 ProductSku 对象
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);

                // e.1 将排序后的 DTO List 转换为 Map List
                List<Map<String, String>> specMapList = sortedSpecDTOs.stream()
                        .map(spec -> Map.of("key", spec.getKey(), "value", spec.getValue()))
                        .collect(Collectors.toList());
                // e.2 调用setter 方法，传入 Map List 对象
                sku.setSpecifications(specMapList);

                sku.setPrice(skuDTO.getPrice());
                sku.setStock(skuDTO.getStock());
                sku.setSkuCode(skuDTO.getSkuCode());
                sku.setImage(skuDTO.getImage());
                sku.setStatus(1); // 默认在售
                sku.setCreateTime(LocalDateTime.now());
                sku.setUpdateTime(LocalDateTime.now());
                savedSkus.add(sku);
            }

            // 批量插入 SKU (在锁内完成插入)
            if (!savedSkus.isEmpty()) {
                productSkuService.saveBatch(savedSkus);
            }
            //----退出临界区----
        }finally {
            //d. 释放分布式锁（必须在finally中确保释放）
            //仅当锁的值是自己设置的值时才删除，防止误删
            if(lockValue.equals(ops.get(lockKey))){
                stringRedisTemplate.delete(lockKey);
            }
        }

        updateProductDisplayPrice(productId);

        //重新获取一次该实体以返回最新的展示价格
        Product createdProduct = this.getById(productId);
        return createdProduct != null ? createdProduct : product; //若查询失败则返回旧对象


        //分布式锁的实现使用了 StringRedisTemplate的setIfAbsent 方法，
        // 这是 Redis 实现分布式锁的常用方式。lockKey 的设计保证了锁的粒度是商品级别。
        // lockValue 使用 UUID 保证了锁的唯一性，防止误删。finally` 块确保锁一定会被尝试释放。
    }

    @Override
    public PageResult<ProductListVO> listProducts(ProductListQueryDTO queryDTO){

        //1.构建分页对象
        Page<Product> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        //2.构建查询条件
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        //只查询上架的商品
        queryWrapper.eq(Product::getStatus, 1);

        //处理分类筛选
        if(queryDTO.getCategoryId() != null && queryDTO.getLevel() != null){
            switch (queryDTO.getLevel()){
                case 1:
                    queryWrapper.eq(Product::getCategory1Id, queryDTO.getCategoryId());
                    break;
                case 2:
                    queryWrapper.eq(Product::getCategory2Id, queryDTO.getCategoryId());
                    break;
                case 3:
                    queryWrapper.eq(Product::getCategory3Id, queryDTO.getCategoryId());
                    break;
                default:
                    throw new BusinessException(4031,"无效的分类");
            }
        }

        if(queryDTO.getKeyWord() != null && !queryDTO.getKeyWord().trim().isEmpty()){
            //TODO:设计文档要求实现搜索名称、详情、标签的关键字，这里只实现了名称
            queryWrapper.like(Product::getName, queryDTO.getKeyWord().trim());
        }

        if(queryDTO.getSortField() != null && !queryDTO.getSortField().isEmpty()){
            boolean isAsc = "asc".equalsIgnoreCase(queryDTO.getSortOrder());

            if("sales".equals(queryDTO.getSortField())){
                queryWrapper.orderBy(true, isAsc, Product::getSales);
            }else if("createTime".equals(queryDTO.getSortField())){
                //按新品排序
                queryWrapper.orderBy(true, isAsc, Product::getCreateTime );
            }else if("price".equals(queryDTO.getSortField())){
                queryWrapper.orderBy(true, isAsc, Product::getDisplayPrice); //使用SPU中的展示价格
            }

            //TODO:更多排序字段



        }else{
            //默认排序（按销量降序）
            queryWrapper.orderByDesc(Product::getSales);
        }

        //3.执行分页查询（只查询SPU信息）
        Page<Product> productPage = this.page(page, queryWrapper);
        List<Product> products = productPage.getRecords();

        //如果当前页没有数据，直接返回空结果
        if(products.isEmpty()){
            return new PageResult<>(productPage.getTotal(), Collections.emptyList());
        }

        //批量查询店铺信息
        //4.获取当前页所有商品的对应storeId列表（同时去重）
        List<Long> storeIds = products.stream()
                .map(Product::getStoreId)
                .distinct()
                .toList();

        //5.根据storeIds 批量查询店铺信息
        Map<Long, Store> storeMap = Collections.emptyMap(); //初始化为空map
        if(!storeIds.isEmpty()){
            List<Store>  stores = storeService.listByIds(storeIds);
            //将其转换为Map，id对应store实体，方便查找
            storeMap = stores.stream()
                    .collect(Collectors.toMap(Store::getId, Function.identity()));
        }

        //4.处理查询结果，转换为VO列表
        final Map<Long, Store> finalStoreMap = storeMap; //lambda表达式需要final或effectively final
        List<ProductListVO> voList = products.stream().map(product -> {
            ProductListVO vo = new ProductListVO();

            // 拷贝 id, name, mainImage, sales, storeId 等 (除了 price)
            BeanUtils.copyProperties(product, vo, "price");

            //一并返回店铺信息
            Store store = finalStoreMap.get(product.getStoreId());
            if(store != null){
                vo.setStoreId(store.getId());
                vo.setStoreName(store.getName());
            }else{
                // 处理店铺不存在或查询失败的情况
                vo.setStoreId(product.getStoreId()); // 至少返回 ID
                vo.setStoreName("未知店铺");
            }

            //5.设置价格为SPU中的展示价格
            vo.setPrice(product.getDisplayPrice());

            // --- 不再需要调用 findMinSkuPrice ---
            // BigDecimal minPrice = findMinSkuPrice(product.getId());
            // vo.setPrice(minPrice);

            return vo;
        }).toList();

        //6.封装并返回PageResult
        return new PageResult<>(productPage.getTotal(), voList);
    }

    @Override
    public PageResult<ProductListVO> listMerchantProducts(Long storeId, MerchantProductListQueryDTO queryDTO){
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            throw new BusinessException(4012,"请先登录");
        }

        //1.校验店铺归属
        Store store = storeService.getById(storeId);
        if(store == null || !store.getUserId().equals(userId)){
            throw new BusinessException(4031, "无权操作该店铺");
        }

        //2.构建分页对象
        Page<Product> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        //3.构建查询条件
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStoreId, storeId);

        //按状态筛选（筛选上架或下架商品
        if(queryDTO.getStatus() != null){
            //由于查找最低价格的方法里面强制只查询上架的，这里还用不了
            //queryWrapper.eq(Product::getStatus, queryDTO.getStatus());
        }

        if(queryDTO.getName() != null && !queryDTO.getName().isEmpty()){
            queryWrapper.like(Product::getName,queryDTO.getName());
        }

        queryWrapper.orderByDesc(Product::getUpdateTime); //商家后台默认按更新时间排序

        //4.执行分页查询（SPU）
        Page<Product> productPage = this.page(page, queryWrapper);
        List<Product> products = productPage.getRecords();

        if(products.isEmpty()){
            return new PageResult<>(productPage.getTotal(), Collections.emptyList());
        }

        // 5. 组装VO (商家列表也需要展示最低价)
        // (此部分逻辑与 public listProducts 类似）

        List<ProductListVO> voList = products.stream().map(product -> {
            ProductListVO vo = new ProductListVO();
            BeanUtils.copyProperties(product, vo, "price"); // 拷贝 id, name, mainImage, sales

            // 设置店铺信息
            vo.setStoreName(store.getName());

            // 5.设置价格为SPU中的展示价格
            vo.setPrice(product.getDisplayPrice());

            return vo;
        }).toList();

        // 6. 封装并返回PageResult
        return new PageResult<>(productPage.getTotal(), voList);
    }

    /**
     * 根据SPU ID查询其下属SKU中的最低价格
     * @param productId SPU ID
     * @return 最低价格，若无SKU则返回0（一般来说只有一个规格的SPU也必须创建一个SKU）
     * @author 周开播
     * @Date 2025年10月24日11:08:46
     */
    private BigDecimal findMinSkuPrice(Long productId){

        //构建查询条件，只查price字段，且SKU的status必须为1
        LambdaQueryWrapper<ProductSku> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductSku::getProductId, productId)
                    .eq(ProductSku::getStatus, 1) //只考虑在售SKU
                    .select(ProductSku::getPrice) //只看价格
                    .orderByDesc(ProductSku::getPrice) //按价格升序
                    .last("LIMIT 1"); //只取第一条（最低价）

        ProductSku minPriceSku = productSkuService.getOne(queryWrapper);
        return (minPriceSku != null) ? minPriceSku.getPrice() : BigDecimal.ZERO;
    }

    @Override
    public ProductDetailVO getProductDetails(Long productId){

        String cacheKey = CACHE_PRODUCT_DETAIL_KEY_PREFIX + productId;

        //----尝试从Redis读取缓存----
        try{
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if(cachedJson != null && !cachedJson.isEmpty()){
                //缓存命中，反序列化JSON并返回
                log.info("商品详情缓存命中：{}", cacheKey);
                try{
                    return objectMapper.readValue(cachedJson, ProductDetailVO.class);
                }catch (JsonProcessingException e){
                    log.error("反序列化商品详情缓存失败：keu={}", cacheKey, e);
                    //反序列化失败不抛出异常，继续下面的查库操作
                }
            }
        }catch (Exception e){
            log.error("读取Redis缓存失败", e);
            //Redis异常，降级处理继续查库
        }
        //--- 缓存未命中或异常，查询数据库 ---
        log.info("商品详情缓存未命中，查询数据库: {}", productId);

        //1.查询SPU信息
        Product product = this.getById(productId);

        //2.检查商品是否存在且已上架
        if(product == null || product.getStatus() == 0){
            // TODO: (优化) 缓存穿透处理：如果确定 ID 不存在，可以在 Redis 存一个特殊值 (如空字符串或特定标记)，并设置较短过期时间，防止反复查库
            throw new BusinessException(4041, "商品不存在或已下架");
        }

        //3.查询该SPU下所有“在售”的SKU（ProductSku）列表
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getStatus, 1);
        List<ProductSku> skus = productSkuService.list(skuWrapper);

        //4.将ProductSku实体列表转换为ProductSkuVo列表
        List<ProductSkuVO> skuVOs = skus.stream().map(sku -> {
            ProductSkuVO skuVO = new ProductSkuVO();
            BeanUtils.copyProperties(sku, skuVO); //快速拷贝同名属性（id, specifications, price, stock, image）
            return skuVO;
        }).toList();

        //5.组装ProductDetailVO
        ProductDetailVO detailVO = new ProductDetailVO();
        //拷贝SPU的属性到detailVO (id, storeId, name, detailContent, sales, mainImage, images)
        BeanUtils.copyProperties(product, detailVO);
        //再填充SKU列表
        detailVO.setSkus(skuVOs);

        //----将查询结果写入Redis缓存----
        try{
            String jsonToCache = objectMapper.writeValueAsString(detailVO);
            //设置过期时间：基础TTL + 随机秒数（0-300）防止缓存雪崩
            long randomSeconds = ThreadLocalRandom.current().nextLong(301); //0-300
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    jsonToCache,
                    CACHE_PRODUCT_DETAIL_TTL_HOURS * 3600 + randomSeconds, //总秒数
                    TimeUnit.SECONDS
            );
            log.info("商品详情写入缓存：{}, TTL: {}h + {}s", cacheKey, CACHE_PRODUCT_DETAIL_TTL_HOURS, randomSeconds);
        }catch (JsonProcessingException e){
            log.error("序列化商品详情失败，productId={}", productId ,e);
            // 序列化失败，不影响返回结果，但缓存未写入
        }catch (Exception e){
            log.error("写入Redis缓存失败", e);
            // Redis 写入异常，不影响返回结果
        }

        return detailVO;

        /** TODO: getProductDetails方法中：
         * 关于缓存等的优化:
         * 设计文档中提到的布隆过滤器、互斥锁等是重要的性能优化手段，但会增加实现的复杂度。
         * 当前的实现没有包含这些优化，是直接查询数据库。这在项目初期或数据量不大时是可行的。
         * 目前先确保基础功能正确可用，后续迭代再根据需要添加缓存等优化措施。
         */

    }

    @Override
    @Transactional
    public void updateProductStatus(Long productId, UpdateStatusDTO updateStatusDTO){
        Long userId = BaseContext.getCurrentId();
        if(userId == null) {
            throw new BusinessException(4012, "请先登录");
        }

        //1.查询商品信息，获取店铺id
        Product product = this.getById(productId);
        if(product == null){
            throw new BusinessException(4041, "商品不存在");
        }

        //2.校验店铺归属
        Store store = storeService.getById(product.getStoreId());
        if(store == null || !store.getUserId().equals(userId)){
            throw new BusinessException(4031, "无权操作该商品");
        }

        //3.更新商品状态
        Product productToUpdate = new Product();
        productToUpdate.setId(productId);
        productToUpdate.setStatus(updateStatusDTO.getStatus());
        product.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(productToUpdate);

        //4.【关键】Redis缓存一致性处理：删除商品详情缓存
        if(updated){
            //SPU 状态变更也可能影响最低价（比如 SPU 下架了，虽然只看在售 SKU，但逻辑上一致性更新更好）
            updateProductDisplayPrice(productId);

            String cacheKey = CACHE_PRODUCT_DETAIL_KEY_PREFIX + productId; //与详情页缓存Key一致
            stringRedisTemplate.delete(cacheKey);
            log.info("商品 {} 状态更新，删除缓存 {}", productId, cacheKey); // 可选日志
        }else{
            //更新失败，可能是并发导致
            log.warn("更新商品 {} 状态失败，可能已被删除或并发修改", productId);
            throw new BusinessException(500, "更新商品状态失败，请重试");
        }
    }

    @Override
    @Transactional
    public void updateProduct(Long productId, ProductUpdateDTO updateDTO){
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(4012, "请先登录");
        }

        //1.查询现有商品信息,校验归属
        Product existingProduct = this.getById(productId);
        if(existingProduct == null){
            throw new BusinessException(4041, "商品不存在");
        }
        Store store = storeService.getById(existingProduct.getStoreId());
        if (store == null || !store.getUserId().equals(userId)) {
            throw new BusinessException(4031, "无权操作该商品");
        }

        //2.更新Product(SPU)信息
        BeanUtils.copyProperties(updateDTO, existingProduct, "id", "storeId", "sales", "status", "createTime");
        existingProduct.setUpdateTime(LocalDateTime.now());
        this.updateById(existingProduct);

        //3.处理SKU列表(增删改)
        String lockKey = "lock:sku:update:product:" + productId;
        String lockValue = UUID.randomUUID().toString();
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        try{
            //尝试获取锁
            Boolean acquired = ops.setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
            if(Boolean.FALSE.equals(acquired)){
                throw new BusinessException(5031,"系统繁忙,请稍后再试");
            }

            //----进入临界区----

            //查询现有的所有SKU ID
            LambdaQueryWrapper<ProductSku> currentSkuQuery = new LambdaQueryWrapper<>();
            currentSkuQuery.eq(ProductSku::getProductId, productId).select(ProductSku::getId);
            Set<Long> existingSkuIds = productSkuService.list(currentSkuQuery).stream()
                    .map(ProductSku::getId)
                    .collect(Collectors.toSet());

            //准备待更新或新增的SKU列表
            List<ProductSku> skusToUpdate = new ArrayList<>();
            List<ProductSku> skusToAdd = new ArrayList<>();
            Set<Long> incomingSkuIds = new HashSet<>();

            // 【关键修改1】用于临时记录本次请求中所有的标准化规格,防止前端传入的SKU之间重复
            Set<String> incomingSpecJsonSet = new HashSet<>();

            for(SkuUpdateDTO skuDTO : updateDTO.getSkus()){
                //标准化规格
                List<SpecificationDTO> sortedSpecDTOs = skuDTO.getSpecifications().stream()
                        .sorted(Comparator.comparing(SpecificationDTO::getKey))
                        .collect(Collectors.toList());
                String standardizedSpecJson = productSkuService.standardizeSpecifications(sortedSpecDTOs);

                // 【关键修改2】检查本次请求中是否有重复规格
                if(incomingSpecJsonSet.contains(standardizedSpecJson)){
                    throw new BusinessException(4092, "提交的 SKU 规格中存在重复: " + standardizedSpecJson);
                }
                incomingSpecJsonSet.add(standardizedSpecJson);

                // 转换规格对象(用于设置实体)
                List<Map<String, String>> specMapList = sortedSpecDTOs.stream()
                        .map(spec -> Map.of("key", spec.getKey(), "value", spec.getValue()))
                        .toList();

                //判断是新增还是修改
                if(skuDTO.getId() != null && skuDTO.getId() > 0){
                    //---修改---
                    //校验该SKU ID是否属于当前商品
                    if(!existingSkuIds.contains(skuDTO.getId())){
                        throw new BusinessException(4031, "SKU ID " + skuDTO.getId() + " 不属于该商品");
                    }

                    //校验规格唯一性(排除自身)
                    if(productSkuService.checkDuplicateSkuExcludeSelf(productId, standardizedSpecJson, skuDTO.getId())){
                        throw new BusinessException(4092, "SKU 规格与其他 SKU 重复: " + standardizedSpecJson);
                    }

                    //构建更新对象
                    ProductSku skuToUpdate = new ProductSku();
                    skuToUpdate.setId(skuDTO.getId());
                    skuToUpdate.setSpecifications(specMapList);
                    skuToUpdate.setPrice(skuDTO.getPrice());
                    skuToUpdate.setStock(skuDTO.getStock());
                    skuToUpdate.setSkuCode(skuDTO.getSkuCode());
                    skuToUpdate.setImage(skuDTO.getImage());
                    skuToUpdate.setUpdateTime(LocalDateTime.now());
                    skusToUpdate.add(skuToUpdate);
                    incomingSkuIds.add(skuDTO.getId());

                }else{
                    //---新增---
                    //校验规格唯一性(无需排除自身)
                    if (productSkuService.checkDuplicateSku(productId, standardizedSpecJson)) {
                        throw new BusinessException(4092, "新增的 SKU 规格与已有 SKU 重复: " + standardizedSpecJson);
                    }

                    //构建新增对象
                    ProductSku skuToAdd = new ProductSku();
                    skuToAdd.setProductId(productId);
                    skuToAdd.setSpecifications(specMapList);
                    skuToAdd.setPrice(skuDTO.getPrice());
                    skuToAdd.setStock(skuDTO.getStock());
                    skuToAdd.setSkuCode(skuDTO.getSkuCode());
                    skuToAdd.setImage(skuDTO.getImage());
                    skuToAdd.setStatus(1);
                    skuToAdd.setCreateTime(LocalDateTime.now());
                    skuToAdd.setUpdateTime(LocalDateTime.now());
                    skusToAdd.add(skuToAdd);
                }
            }

            //找出需要删除的SKU ID
            Set<Long> skusToDeleteIds = new HashSet<>(existingSkuIds);
            skusToDeleteIds.removeAll(incomingSkuIds);

            //【关键修改3】按顺序执行数据库操作: 先删除,再更新,最后新增
            if(!skusToDeleteIds.isEmpty()){
                productSkuService.removeByIds(skusToDeleteIds);
                log.info("删除 SKU: {}", skusToDeleteIds);
            }
            if(!skusToUpdate.isEmpty()){
                productSkuService.updateBatchById(skusToUpdate);
                log.info("更新 {} 个 SKU", skusToUpdate.size());
            }
            if (!skusToAdd.isEmpty()) {
                productSkuService.saveBatch(skusToAdd);
                log.info("新增 {} 个 SKU", skusToAdd.size());
            }

            //---退出临界区---

        }finally {
            //释放锁
            if(lockValue.equals(ops.get(lockKey))){
                stringRedisTemplate.delete(lockKey);
            }
        }

        updateProductDisplayPrice(productId);

        //删除缓存
        String cacheKey = CACHE_PRODUCT_DETAIL_KEY_PREFIX + productId;
        stringRedisTemplate.delete(cacheKey);
        log.info("商品 {} 更新成功,删除缓存 {}", productId, cacheKey);
    }
}
