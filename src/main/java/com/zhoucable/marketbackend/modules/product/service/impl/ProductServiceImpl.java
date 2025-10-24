package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private StoreService storeService; //用于校验店铺归属

    @Autowired
    private ProductSkuService productSkuService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate; //用于分布式锁

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
        return product; //返回创建的商品信息

        //分布式锁的实现使用了 StringRedisTemplate的setIfAbsent 方法，
        // 这是 Redis 实现分布式锁的常用方式。lockKey 的设计保证了锁的粒度是商品级别。
        // lockValue 使用 UUID 保证了锁的唯一性，防止误删。finally` 块确保锁一定会被尝试释放。
    }

    @Override
    public PageResult<ProductListVO> listProducts(ProductListQueryDTO queryDTO){

        //1.构建分页对象
        Page<Product> page = new Page<>(queryDTO.getPage(), queryDTO.getPage());

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

        // TODO: 添加排序逻辑 (如果 queryDTO 中添加了 sortField)
        // queryWrapper.orderByDesc(Product::getSales); // 例如按销量降序

        //3.执行分页查询（只查询SPU信息）
        Page<Product> productPage = this.page(page, queryWrapper);

        //4.处理查询结果，转换为VO列表
        List<ProductListVO> voList = productPage.getRecords().stream().map(product -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(product.getId());
            vo.setName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setSales(product.getSales());

            //5.查询该SPU下所有SKU的最低价格
            BigDecimal minPrice = findMinSkuPrice(product.getId());
            vo.setPrice(minPrice);

            return vo;
        }).toList();

        //6.封装并返回PageResult
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

        //1.查询SPU信息
        Product product = this.getById(productId);

        //2.检查商品是否存在且已上架
        if(product == null || product.getStatus() == 0){
            throw new BusinessException(404, "商品不存在或已下架");
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

        return detailVO;

        /** TODO: getProductDetails方法中：
         * 关于缓存等的优化:
         * 设计文档中提到的 Redis 缓存、布隆过滤器、互斥锁等是重要的性能优化手段，但会增加实现的复杂度。
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
            String cacheKey = "product:detail:" + productId; //与详情页缓存Key一致
            stringRedisTemplate.delete(cacheKey);
            //log.info("商品 {} 状态更新，删除缓存 {}", productId, cacheKey); // 可选日志
        }else{
            //更新失败，可能是并发导致
            //log.warn("更新商品 {} 状态失败，可能已被删除或并发修改", productId);
            throw new BusinessException(500, "更新商品状态失败，请重试");
        }
    }
}
