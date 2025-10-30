package com.zhoucable.marketbackend.modules.order.vo;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单详情VO中包含的商品项详细信息
 * @author 周开播
 * @Date 2025年10月30日09:53:48
 */
@Data
public class OrderItemDetailVO {
    private Long orderItemId; //订单明细项ID
    private Long skuId;
    private Long productId;
    private Integer quantity;
    private BigDecimal priceSnapshot; //购买时单价
    private Integer reviewStatus; //评价状态（0：待评价，1：已评价）

    //---从SKU快照中获取的信息----
    private String productName;
    private String specifications; //格式化后的规格字符串
    private String image;

    // --- (可选) 直接返回原始快照给前端 ---
    // private Map<String, Object> skuSnapshot;
}
