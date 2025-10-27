package com.zhoucable.marketbackend.modules.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.merchant.dto.StoreCreateDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.mapper.StoreMapper;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.user.entity.User;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import com.zhoucable.marketbackend.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    @Autowired
    private UserService userService;

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

    /**
     * （供商家使用）商家主动创建新店铺
     */
    @Override
    public Store createStoreByMerchant(StoreCreateDTO createDTO) {

        //1.获取当前登录用户的id
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            throw new BusinessException(4012, "请先登录");
        }

        //2.校验用户角色是否为商家（Role = 1）
        User currentUser = userService.getById(userId);
        if(currentUser == null || currentUser.getRole() != 1){
            throw new BusinessException(4031, "只有商家才能创建新店铺");
        }

        //3.调用核心创建逻辑（复用之前的方法）
        //这里传入的是当前登录的商家的ID
        return this.createStore(userId, createDTO.getName(), createDTO.getDescription());
    }

    @Override
    public List<Store> listStoresByCurrentUser(){

        //1.获取当前登录的用户id
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            throw new BusinessException(4012, "请先登录");
        }

        //2.校验用户角色是否为商家
        User currentUser = userService.getById(userId);
        if(currentUser == null || currentUser.getRole() != 1){
            throw new BusinessException(4031, "只有商家能够查询店铺列表");
        }

        //3.根据userId查询store表
        LambdaQueryWrapper<Store> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Store::getUserId, userId)
                .orderByDesc(Store::getCreateTime); //按创建时间排序

        return this.list(queryWrapper);
    }
}
