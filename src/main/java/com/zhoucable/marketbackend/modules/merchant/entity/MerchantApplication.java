package com.zhoucable.marketbackend.modules.merchant.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家入驻申请表
 * @author 周开播
 * @Date 2025/10/22 16:39
 */
@Data
@TableName("merchant_application")
public class MerchantApplication {

    private Long id;

    private Long userId;

    private String storeName;

    private String description;

    private Integer status; //0：待审核，1：已通过，2：已拒绝

    private String reason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
