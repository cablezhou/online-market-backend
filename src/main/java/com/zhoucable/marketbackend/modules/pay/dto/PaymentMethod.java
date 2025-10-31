package com.zhoucable.marketbackend.modules.pay.dto;

import lombok.Getter;

/**
 * 支付方式枚举
 * @author 周开播
 * @Date 2025年10月31日10:31:05
 */
@Getter
public enum PaymentMethod {
    //code对应数据库中的1
    WECHAT_PAY(1),
    //code对应数据库中的2
    ALI_PAY(2);

    private final int code;

    PaymentMethod(int code){
        this.code = code;
    }
}
