package com.zhoucable.marketbackend.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单列表VO，包含单个商品的简略信息（仅用于返回列表查询结果）
 */
@Data
public class OrderItemInListVO {
    private Long skuId;
    //---从SkuSnapshot获取---
    private String productName;
    private String specifications;
    private String image;
    //-----------------------
    private Integer quantity;
    private BigDecimal price; //从priceSnapshot获取
}
