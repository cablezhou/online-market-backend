package com.zhoucable.marketbackend.modules.pay.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水表-实体
 * @author 周开播
 * @Date 2025年10月31日10:25:30
 */
@Data
@TableName("refund_transaction")
public class RefundTransaction {

    private Long id; //退款流水id

    /**
     * 关联的售后单
     */
    private Long refundOrderId;

    /**
     * 关联的售后单号
     */
    private String refundOrderNumber;

    /**
     * 关联的原支付流水号（第三方的transaction_id）
     */
    private String paymentTransactionId;

    /**
     * 第三方支付渠道返回的退款单号
     */
    private String refundId;

    private BigDecimal amount; //退款金额

    /**
     * 退款状态（0：处理中，1：成功，2：失败）
     */
    private Integer status;

    private String reason; //退款原因

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
