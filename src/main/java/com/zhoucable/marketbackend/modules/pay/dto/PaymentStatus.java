package com.zhoucable.marketbackend.modules.pay.dto;

import lombok.Getter;

/**
 * 支付状态枚举
 * @author 周开播
 * @Date 2025年10月31日10:33:57
 */
@Getter
public enum PaymentStatus {
    PENDING(0), //退款中
    SUCCESS(1), //成功
    FAILED(2); //失败

    private final int code;

    PaymentStatus(int code){
        this.code = code;
    }
}
