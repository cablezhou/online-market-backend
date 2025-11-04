package com.zhoucable.marketbackend.modules.pay.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentStatusVO;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentSubmitVO;
import com.zhoucable.marketbackend.modules.pay.VO.RefundResultVO;
import com.zhoucable.marketbackend.modules.pay.dto.PaymentSubmitDTO;
import com.zhoucable.marketbackend.modules.pay.entity.PaymentTransaction;
import com.zhoucable.marketbackend.modules.refund.entity.RefundOrder;

/**
 * 支付服务接口
 * @author 周开播
 * @Date 2025年10月31日16:09:46
 */
public interface PaymentService extends IService<PaymentTransaction> {

    /**
     * (FR-PAY-001)
     * 用户发起支付
     * @param userId 用户ID
     * @param paymentSubmitDTO 支付请求信息
     * @return 返回给前端调起支付SDK所需的信息
     */
    PaymentSubmitVO submitPayment(Long userId, PaymentSubmitDTO paymentSubmitDTO);

    /**
     * (FR-PAY-002)
     * 处理支付成功的核心逻辑
     * @param parentOrderNumber 父订单号
     * @param paymentMethod 支付方式
     * @param transactionId 第三方流水号 (可选)
     */
    void processSuccessfulPayment(String parentOrderNumber, Integer paymentMethod, String transactionId);

    /**
     * 主动查询支付状态（FR-PAY-003）
     * @param userId 用户id
     * @param parentOrderNumber 父订单号
     * @return 支付状态VO
     * @Date 2025年11月3日10:37:15
     */
    PaymentStatusVO queryPaymentStatus(Long userId, String parentOrderNumber);

    /**
     * 内部方法，向第三方支付渠道申请执行退款
     * 由RefundOrderService调用
     * @param refundOrder 售后单
     * @return 退款受理结果
     */
    RefundResultVO executeRefund(RefundOrder refundOrder);
}
