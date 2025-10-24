package com.zhoucable.marketbackend.modules.product.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产品表
 * @author 周开播
 * @Date 2025年10月23日15:54:58
 */
@Data
@TableName(value = "product", autoResultMap = true) // 开启 autoResultMap 以支持 TypeHandler
public class Product {

    private Long id;
    private Long storeId; //归属的商店id
    private Long category1Id;
    private Long category2Id;
    private Long category3Id;
    private String name;

    //使用JacksonTypeHandler处理Json字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> detailContent; //图文详情

    private Integer sales;
    private String mainImage;

    //使用JacksonTypeHandler处理Json字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images; //轮播图

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
