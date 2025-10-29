package com.zhoucable.marketbackend.modules.order.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 父订单表-实体
 * @author 周开播
 * @Date 2025年10月29日10:05:37
 */
@Data
@TableName("parent_order")
public class ParentOrder {
    private Long id; //主键id

    private String parentOrderNumber; //父订单号，全局唯一

    private Long userId; //下单用户id

    private BigDecimal totalAmount; //该父订单下所有子订单的总金额

    private BigDecimal paymentAmount; //用户实际支付总金额

    private Integer paymentMethod; //支付方式

    private LocalDateTime paymentTime; //支付成功时间

    private LocalDateTime createTime; //记录创建时间

    private LocalDateTime updateTime; //记录最后更新时间
}
