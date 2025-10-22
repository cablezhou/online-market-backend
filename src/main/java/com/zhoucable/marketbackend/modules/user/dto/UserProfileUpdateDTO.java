package com.zhoucable.marketbackend.modules.user.dto;


import lombok.Data;

/**
 * 传递用户修改个人信息请求的DTO
 */

@Data
public class UserProfileUpdateDTO {

    private String nickname;
    //TODO:未来可拓展其他字段，如头像等
}
