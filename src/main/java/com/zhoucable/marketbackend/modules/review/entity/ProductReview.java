package com.zhoucable.marketbackend.modules.review.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价表
 * @author 周开播
 * @Date 2025年11月3日14:25:41
 */
@Data
@TableName(value = "product_review", autoResultMap = true)
public class ProductReview {

    private Long id;
    private Long userId;
    private Long storeId;
    private Long orderId; //对应子订单id
    private Long orderItemId; //订单明细id
    private Long productId;
    private Long skuId;
    private Integer rating; ///星级评分（1-5）
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;//评价图片URL列表

    private Integer status; //状态（0：待审核，1：显示，2：隐藏）
    private LocalDateTime createTime;
}
