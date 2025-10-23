package com.zhoucable.marketbackend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册时的DTO
 * @author 周开播
 * 2025/10/21 15:29
 */

@Data
public class UserRegisterDTO {


    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Length(min = 8, message = "密码长度不能低于8位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "密码必须包含大小写字母和数字")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
