package com.zhoucable.marketbackend.modules.order.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表返回给前端的VO（对应一个子订单）
 * @author 周开播
 * @Date 2025年10月29日16:57:35
 */
@Data
public class OrderListVO {

    private Long orderId; //子订单id
    private String orderNumber; //子订单号
    private Long storeId;
    private String storeName; //店铺名称（需关联查询）
    private BigDecimal totalAmount; //子订单总金额
    private Integer status; //子订单状态码

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //格式化时间
    private LocalDateTime createTime; //下单时间

    // 设计文档中提到的 mainProductImage 和 totalProducts 可以通过 items 列表获取
    private List<OrderItemInListVO> items; // 订单中的商品项 (简略信息)

    // 未来根据需要添加其他字段，例如父订单号
    // private String parentOrderNumber;
}
