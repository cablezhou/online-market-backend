package com.zhoucable.marketbackend.modules.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单详情返回给前端的ＶＯ（对应一个子订单的完整信息）
 * @author 周开播
 * @Date 2025年10月30日09:57:24
 */
@Data
public class OrderDetailVO {

    //----订单基本信息----
    private Long orderId; //子订单ID
    private String orderNumber; //子订单号
    private String parentOrderNumber; //父订单号（关联查询获得）
    private Long storeId;
    private String storeName; //店铺名称（关联查询）
    private BigDecimal totalAmount; //子订单总金额
    private Integer status; //子订单状态码
    private String note; //用户备注

    //---时间信息----
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 下单时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime; // 支付时间 (从父订单获取)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shippingTime; // 发货时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime; // 完成时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime; // 取消时间

    //----地址与物流----
    private Map<String, Object> addressSnapshot; //收货地址快照
    private Map<String, Object> shippingInfo; //物流信息

    //----商品列表----
    private List<OrderItemDetailVO> items; // 订单中的商品项 (详细信息)
}
