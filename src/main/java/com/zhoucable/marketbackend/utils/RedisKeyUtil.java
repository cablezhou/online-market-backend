package com.zhoucable.marketbackend.utils;

import com.zhoucable.marketbackend.modules.user.dto.SmsCodeType;

/**
 * Redis Key 统一管理工具类
 * @author 周开播
 * @Date 2025/10/22 16:03
 */
public class RedisKeyUtil {

    // Key 的前缀，用于区分不同业务
    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final String LOGIN_TOKEN_PREFIX = "login:token:";
    private static final String CART_USER_PREFIX = "cart:user:";

    /**
     * 生成短信验证码的 Key
     * @param type 业务类型 (LOGIN 或 RESET_PASSWORD)
     * @param phone 手机号
     * @return Redis Key (e.g., "sms:code:LOGIN:138...")
     */
    public static String getSmsCodeKey(SmsCodeType type, String phone) {
        // 使用 .name() 获取枚举的字符串表示
        return SMS_CODE_PREFIX + type.name() + ":" + phone;
    }

    /**
     * 生成短信发送频率限制的 Key
     * @param type 业务类型
     * @param phone 手机号
     * @return Redis Key (e.g., "sms:limit:LOGIN:138...")
     */
    public static String getSmsLimitKey(SmsCodeType type, String phone) {
        return SMS_LIMIT_PREFIX + type.name() + ":" + phone;
    }

    /**
     * 生成用户登录 Token 的 Key
     * @param token JWT Token
     * @return Redis Key (e.g., "login:token:ey...")
     */
    public static String getLoginTokenKey(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }

    /**
     * 生成用户购物车缓存的Key
     * @param userId 用户id
     * @return Redis Key（e.g. "cart:user:123")
     */
    public static String getCartUserKey(Long userId){
        return CART_USER_PREFIX + userId;
    }
}