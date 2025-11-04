package com.zhoucable.marketbackend.modules.pay.VO;


import lombok.Data;

/**
 * 退款申请受理结果VO
 * FR-PAY-004的返回对象
 * @author 周开播
 * @Date 2025年11月3日10:34:16
 */
@Data
public class RefundResultVO {

    /**
     * 退款申请是否被支付渠道受理
     */
    private boolean accepted;

    /**
     * 本地创建的退款流水号（refund_transaction.id）
     */
    private Long refundTransactionId;

    /**
     * 附加信息（例如“受理成功”）
     */
    private String message;
}
