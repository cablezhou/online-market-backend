package com.zhoucable.marketbackend.modules.pay.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起支付请求DTO（FR-PAY-001）
 */
@Data
public class PaymentSubmitDTO {

    /**
     * 要支付的订单号（ParentOrderNumber）
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNumber;

    /**
     * 支付方式（例如：1：微信，2：支付宝）
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;
}
