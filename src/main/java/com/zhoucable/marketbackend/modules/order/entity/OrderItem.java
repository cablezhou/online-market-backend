package com.zhoucable.marketbackend.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单明细表-实体（子订单下的单独一项商品）
 * @author 周开播
 * @Date 2025年10月29日10:15:27
 */
@Data
@TableName(value = "order_item", autoResultMap = true)
public class OrderItem {

    private Long id;

    private Long orderId; //所属子订单id

    private Long storeId; //商品所属店铺id

    private Long productId; //商品SPU id

    private Long skuId; //商品SKU id

    private Integer quantity; //购买数量

    private BigDecimal priceSnapshot; //购买时SKU的单价快照

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> skuSnapshot; //SKU信息快照（品名，规格，图片等）

    private Integer reviewStatus; //评价状态（0：未评价，1：已评价）

    /**
     * 新增字段
     * 售后状态（0：未售后，1：售后中，2：售后完成）
     * @Date 2025年10月31日14:42:53
     */
    private Integer refundStatus;

    /**
     * 新增字段
     * 已申请售后的数量
     * @Date 2025年10月31日14:43:19
     */
    private Integer refundedQuantity;

}
