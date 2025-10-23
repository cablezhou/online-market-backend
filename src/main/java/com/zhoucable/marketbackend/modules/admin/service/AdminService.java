package com.zhoucable.marketbackend.modules.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.admin.dto.AdminReviewDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;

/**
 * 管理员服务
 * @author 周开播
 * @Date 2025/10/23 9:50
 */
//IService的泛型先用MerchantApplication，因为主要是操作它
public interface AdminService extends IService<MerchantApplication> {

    /**
     * 审核商家申请
     * @param applicationId 审核ID
     * @param reviewDTO 审核操作DTO
     * @author 周开播
     * @Date 2025/10/23 9:54
     */
    void reviewApplication(Long applicationId, AdminReviewDTO reviewDTO);
}
