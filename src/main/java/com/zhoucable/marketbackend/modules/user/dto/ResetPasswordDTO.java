package com.zhoucable.marketbackend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 重置密码的DTO
 * @author 周开播
 * @Date 2025/10/22 15:32
 */

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Length(min = 8, message = "密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "密码必须包含大小写字母和数字")
    private String newPassword;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
