package com.zhoucable.marketbackend.modules.refund.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.refund.dto.RefundApplyDTO;
import com.zhoucable.marketbackend.modules.refund.dto.RefundReviewDTO;
import com.zhoucable.marketbackend.modules.refund.entity.RefundOrder;

/**
 * 售后单服务接口
 * @author 周开播
 * @Date 2025年10月31日15:01:31
 */
public interface RefundOrderService extends IService<RefundOrder> {

    /**
     * 用户申请售后
     * @param userId 申请人id
     * @param applyDTO 申请信息
     */
    void applyForRefund(Long userId, RefundApplyDTO applyDTO);

    /**
     * 商家审核售后申请
     * @param merchantUserId 商家用户ID
     * @param refundOrderId 售后单ID
     * @param refundReviewDTO 审核操作
     */
    void reviewRefund(Long merchantUserId, Long refundOrderId, RefundReviewDTO refundReviewDTO);
}
