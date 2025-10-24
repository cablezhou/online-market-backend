package com.zhoucable.marketbackend.modules.product.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品的SKU表
 * @author 周开播
 * @Date 2025年10月23日15:59:10
 */
@Data
@TableName(value = "product_sku", autoResultMap = true)
public class ProductSku {

    private Long id;
    private Long productId;
    private String skuCode;

    // 使用 JacksonTypeHandler 处理 JSON 字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> specifications; // 规格的JSON字段

    private BigDecimal price; //使用BigDecimal处理价格
    private Integer stock;
    private String image;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
