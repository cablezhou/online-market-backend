package com.zhoucable.marketbackend.modules.address.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

/**
 * 用户地址表-实体
 * @author 周开播
 * @Date 2025年10月28日15:55:39
 */
@Data
@TableName("user_address")
public class UserAddress {
    private Long id;
    private Long userId;
    private String recipientName; //收货人姓名
    private String phone;
    private String province;
    private String city;
    private String district; //区、县
    private String detailAddress; //详细地址
    private Integer isDefault; //是否为默认地址（0：否，1：是）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
