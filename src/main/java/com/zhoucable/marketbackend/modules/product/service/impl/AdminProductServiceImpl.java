package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, productId); //定位要更新的记录

        updateWrapper.set(Product::getStatus, updateDTO.getStatus());
        updateWrapper.set(Product::getUpdateTime, LocalDateTime.now());

        if(updateDTO.getStatus() == 0){ //管理员执行下架
            updateWrapper.set(Product::getAdminReason, updateDTO.getReason());
            updateWrapper.set(Product::getForceOffline, 1);
            log.warn("管理员强制下架商品 {}, 理由: {}", productId, updateDTO.getReason());
        }else{ //管理员执行上架
            updateWrapper.set(Product::getAdminReason, null);
            updateWrapper.set(Product::getForceOffline, 0);
            log.info("管理员上架商品 {}", productId);
        }


/*      原本的更新逻辑，上架时adminReason无法重置为null，因为MybatisPlus的updateById方法默认会忽略null属性
        Product productToUpdate = new Product();
        productToUpdate.setId(productId);
        productToUpdate.setStatus(updateDTO.getStatus());
        productToUpdate.setUpdateTime(LocalDateTime.now());

        //填充下架原因以及强制下架标识（若为下架）
        if(updateDTO.getStatus() == 0){
            productToUpdate.setAdminReason(updateDTO.getReason());
            productToUpdate.setForceOffline(1); //标记为被强制下架
            log.warn("管理员强制下架商品{}, 理由{}", productId, updateDTO.getReason());
        }else{ //管理员解除强制下架状态
            productToUpdate.setAdminReason(null);
            productToUpdate.setForceOffline(0);
            log.info("管理员上架商品 {}", productId);
        }*/

        boolean updated = this.update(updateWrapper);
        //删除Redis缓存
        if(updated){

            //TODO:考虑在此处再调用updateProductDisplayPrice更新一次展示价格
            //目前暂不添加，因为无法确定AdminProductServiceImpl 和 ProductServiceImpl
            //是否在同一个事务下。未来实现该功能可能需要考虑将updateProductDisplayPrice提升
            //到ProductService接口。PS:需要同步修改updateProductDisplayPrice的访问修饰符
            //productServiceImpl.updateProductDisplayPrice(productId);


            String cacheKey = CACHE_PRODUCT_DETAIL_KEY_PREFIX + productId;
            stringRedisTemplate.delete(cacheKey);
            log.info("管理员更新商品{}状态，删除缓存{}", productId, cacheKey);
        }else{
            log.warn("管理员更新商品{}状态失败",productId);
            throw new BusinessException(500,"更新商品状态失败，请重试");
        }
    }

}
