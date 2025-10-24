package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.product.dto.ProductCreateDTO;
import com.zhoucable.marketbackend.modules.product.dto.SkuDTO;
import com.zhoucable.marketbackend.modules.product.dto.SpecificationDTO;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.mapper.ProductMapper;
import com.zhoucable.marketbackend.modules.product.mapper.ProductSkuMapper;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import com.zhoucable.marketbackend.modules.product.service.ProductSkuService;
import com.zhoucable.marketbackend.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
