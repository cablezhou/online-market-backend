package com.zhoucable.marketbackend.modules.product.dto;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表中每一项商品的视图对象
 * @author 周开播
 * @Date 2025年10月24日10:44:47
 */
@Data
public class ProductListVO {

    private Long id; //商品SPU ID

    private Long storeId; //所属店铺id

    private String storeName; //所属店铺名称

    private String name; //商品名称

    private BigDecimal price; //展示价格（所有SPU中的最低价）

    private String mainImage; //主图url

    private Integer sales; //商品销量（SPU销量）
}
