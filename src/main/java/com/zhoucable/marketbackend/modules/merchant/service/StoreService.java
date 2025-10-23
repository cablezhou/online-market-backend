package com.zhoucable.marketbackend.modules.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;

/**
 * 关于店铺的服务
 * @author 周开播
 * @Date 2025/10/23 9:32
 */

public interface StoreService extends IService<Store> {

    /**
     * 根据申请信息创建店铺（需管理员审核）
     * @param userId 申请者id
     * @param storeName 店铺名称
     * @param description 描述
     * @return 店铺对象
     */
    Store createStore(Long userId, String storeName, String description);
}
