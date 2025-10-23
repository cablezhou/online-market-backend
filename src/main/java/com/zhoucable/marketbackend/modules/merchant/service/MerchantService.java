package com.zhoucable.marketbackend.modules.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.merchant.dto.MerchantApplyDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;

/**
 * 商家申请核心逻辑的实现
 * @author 周开播
 * @Date 2025/10/22 16:49
 */
public interface MerchantService extends IService<MerchantApplication> {

    /**
     * 用户提交商家入驻申请
     * @param applyDTO 申请信息
     */
    void apply(MerchantApplyDTO applyDTO);
}
