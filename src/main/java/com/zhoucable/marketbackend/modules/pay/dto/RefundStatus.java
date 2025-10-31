package com.zhoucable.marketbackend.modules.pay.dto;

import lombok.Getter;

/**
 * 退款状态枚举
 * @author 周开播
 * @Date 2025年10月31日10:34:37
 */
@Getter
public enum RefundStatus {
    PENDING(0), // 处理中
    SUCCESS(1), // 成功
    FAILED(2);  // 失败

    private final int code;

    RefundStatus(int code) {
        this.code = code;
    }
}
