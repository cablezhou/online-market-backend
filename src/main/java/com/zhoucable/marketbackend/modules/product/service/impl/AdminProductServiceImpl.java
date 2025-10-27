package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.product.dto.AdminUpdateStatusDTO;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.mapper.ProductMapper;
import com.zhoucable.marketbackend.modules.product.service.AdminProductService;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.LocalDateTime;

@Service
@Slf4j
public class AdminProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements AdminProductService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //Redis缓存Key前缀和过期时间
    private static final String CACHE_PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final long CACHE_PRODUCT_DETAIL_TTL_HOURS = 24; // 基础 TTL


    @Override
    @Transactional
    public void adminUpdateProductStatus(Long productId, AdminUpdateStatusDTO updateDTO){

        //1.管理员操作，无需校验店铺归属，直接操作商品
        Product product = this.getById(productId);
        if(product == null) {
            throw new BusinessException(4041,"商品不存在");
        }

        //2.更新商品状态
        Product productToUpdate = new Product();
        productToUpdate.setId(productId);
        productToUpdate.setStatus(updateDTO.getStatus());
        productToUpdate.setUpdateTime(LocalDateTime.now());

        //TODO:在product表中存储管理员强制下架的原因（reason）字段，对于管理员强制下架的商品，商家无法再自行上架。
        // log.warn("管理员操作商品 {} 状态为 {}, 理由: {}", productId, updateDTO.getStatus(), updateDTO.getReason());

        boolean updated = this.updateById(productToUpdate);

        //删除Redis缓存
        if(updated){
            String cacheKey = CACHE_PRODUCT_DETAIL_KEY_PREFIX + productId;
            stringRedisTemplate.delete(cacheKey);
            log.info("管理员更新商品{}状态，删除缓存{}", productId, cacheKey);
        }else{
            log.warn("管理员更新商品{}状态失败",productId);
            throw new BusinessException(500,"更新商品状态失败，请重试");
        }
    }

}
