package com.zhoucable.marketbackend.modules.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.merchant.dto.StoreCreateDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;

/**
 * 关于店铺的服务
 * @author 周开播
 * @Date 2025/10/23 9:32
 */

public interface StoreService extends IService<Store> {

    /**
     * （管理员使用）管理员通过用户“成为商家”的申请后，为其创建默认店铺
     * @param userId 申请者id
     * @param storeName 店铺名称
     * @param description 描述
     * @return 店铺对象
     */
    Store createStore(Long userId, String storeName, String description);

    /**
     * (供商家使用)商家主动创建新店铺
     * @param createDTO 店铺创建信息
     * @return store对象
     * @author 周开播
     * @Date 2025年10月23日11:26:05
     */
    Store createStoreByMerchant(StoreCreateDTO createDTO);
}
