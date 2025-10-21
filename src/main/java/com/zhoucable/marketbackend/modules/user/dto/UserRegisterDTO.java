package com.zhoucable.marketbackend.modules.user.dto;

import lombok.Data;

/**
 * 用户注册时的DTO
 * @author 周开播
 * 2025/10/21 15:29
 */

@Data
public class UserRegisterDTO {

    private String phone;

    private String password;

    private String nickname;
}
