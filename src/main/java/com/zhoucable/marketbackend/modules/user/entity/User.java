package com.zhoucable.marketbackend.modules.user.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 * @author 周开播
 * 2025/10/21 15:10
 */
@Data
@TableName("user")
public class User {

    private Long id; //用户id，自增主键

    private String phone; //手机号，唯一

    private String password; //加密后的密码

    private String nickname; //用户昵称

    private Integer role; //用户角色（0：普通用户，1：商家，2：管理员）

    private LocalDateTime createTime; //记录创建时间

    private LocalDateTime updateTime; //记录更新时间
}
