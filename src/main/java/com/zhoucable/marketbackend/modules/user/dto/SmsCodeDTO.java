package com.zhoucable.marketbackend.modules.user.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 只传递手机号的DTO，用于验证码登录
 * @author 周开播
 * @Date 2025.10.22 11:14
 */
@Data
public class SmsCodeDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 验证码类型：“LOGIN” 或 “RESET_PASSWORD”
     * 用于区分用户是在登录还是在重置密码
     * 若为重置密码，则在发送验证码的流程中需要先检验用户是否存在
     * @author 周开播
     * @Date 2025/10/22 15:18
     */
    @NotBlank(message = "验证码类型不能为空")
    private String type;
}
