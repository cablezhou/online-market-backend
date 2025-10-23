package com.zhoucable.marketbackend.modules.user.dto;

/**
 * 短信验证码的业务类型枚举
 * @author 周开播
 * @Date 2025/10/22 16:02
 */
public enum SmsCodeType {
    /**
     * 用于登录/注册场景
     */
    LOGIN,

    /**
     * 用于重置密码场景
     */
    RESET_PASSWORD
}