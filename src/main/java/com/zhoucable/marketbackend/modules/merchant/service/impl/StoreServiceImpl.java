package com.zhoucable.marketbackend.modules.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.mapper.StoreMapper;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    /**
     * 创建店铺
     */
    @Override
    public Store createStore(Long userId, String storeName, String description){

        //1.检查店铺名是否已被占用（全局）
        LambdaQueryWrapper<Store> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(Store::getName, storeName);
        if(this.count(queryWrapper) > 0){
            throw new BusinessException(4093, "店铺名称" + storeName + "已被占用");
        }


        //2.检查该用户是否已拥有同名店铺
        LambdaQueryWrapper<Store> queryWrapperUser = new LambdaQueryWrapper<>();
        queryWrapperUser
                .eq(Store::getUserId, userId)
                .eq(Store::getName, storeName);
        if(this.count(queryWrapperUser) > 0){
            throw new BusinessException(4094, "您已拥有同名店铺");
        }

        //3.创建店铺
        Store newStore = new Store();
        newStore.setUserId(userId);
        newStore.setName(storeName);
        newStore.setDescription(description);
        newStore.setCreateTime(LocalDateTime.now());
        newStore.setUpdateTime(LocalDateTime.now());

        this.save(newStore);
        return newStore;
    }
}
