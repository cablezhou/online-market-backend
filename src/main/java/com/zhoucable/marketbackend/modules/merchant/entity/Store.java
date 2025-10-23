package com.zhoucable.marketbackend.modules.merchant.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

/**
 * 店铺表
 * @author 周开播
 * @Date 2025/10/22 16:41
 */
@Data
@TableName("store")
public class Store {

    private Long id;

    private Long userId; //店主

    private String name; //店铺名

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
