package com.zhoucable.marketbackend.modules.pay.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询支付状态返回VO
 * FR-PAY-003
 * @author 周开播
 * @Date 2025年11月3日09:56:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusVO {

    /**
     * 订单号
     */
    private String orderNumber;

    /**
     * 支付状态
     * （0：处理中/待支付，1：成功，2：失败）
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String message;
}
