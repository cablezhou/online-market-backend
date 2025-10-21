package com.zhoucable.marketbackend.modules.user.dto;


import lombok.Data;

/**
 * 用户登录DTO（从前端接收）
 * @author 周开播
 * @Date 2025/10/21 16:43
 *
 */

@Data
public class UserLoginDTO {
    private String phone;
    private String password;
}
