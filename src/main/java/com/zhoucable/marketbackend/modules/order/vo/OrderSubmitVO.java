package com.zhoucable.marketbackend.modules.order.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单提交成功后返回给前端的VO
 * @author 周开播
 * @Date 2025年10月29日10:32:12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVO {

    private String parentOrderNumber; //返回父订单号给用户（文档用orderNumber，这里优化为父订单号）

    private BigDecimal totalAmount; //订单总金额

    private List<String> subOrderNumbers; //同时返回所有子订单号；
}
