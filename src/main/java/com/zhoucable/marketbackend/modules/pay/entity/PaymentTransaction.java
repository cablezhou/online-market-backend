package com.zhoucable.marketbackend.modules.pay.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表-实体
 * @author 周开播
 * @Date 2025年10月31日10:21:200
 */
@Data
@TableName("payment_transaction")
public class PaymentTransaction {

    private Long id; //流水id

    /**
     * 关联的商户订单号
     * 应为ParentOrderNumber（一次支付对应一个父订单）
     */
    private String orderNumber;

    /**
     * 第三方支付渠道返回的流水号
     */
    private String transactionId;

    private BigDecimal amount; //支付金额

    /**
     * 支付状态（0：处理中，1：成功，2：失败）
     */
    private Integer status;

    /**
     * 支付方式（例如：1：微信，2：支付宝
     */
    private Integer paymentMethod;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
