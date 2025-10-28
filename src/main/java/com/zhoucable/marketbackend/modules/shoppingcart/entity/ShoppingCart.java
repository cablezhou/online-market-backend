package com.zhoucable.marketbackend.modules.shoppingcart.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 购物车实体
 * @author 周开播
 * @Date 2025年10月28日10:40:15
 */
@Data
@TableName("shopping_cart")
public class ShoppingCart {

    private Long id;

    private Long userId;

    private Long skuId; //加入的商品SKU id

    private Integer quantity; //商品数量

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
