package com.zhoucable.marketbackend.modules.refund.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后表单-实体
 * @author 周开播
 * @Date 2025年10月31日14:49:52
 */
@Data
@TableName("refund_order")
public class RefundOrder {

    private Long id;

    private String refundOrderNumber; //售后单编号（全局唯一）

    private Long userId; //申请用户id

    private Long storeId; //商品所属店铺id

    private Long orderId; //关联的子订单id（orders.id）

    private Long orderItemId; //关联的订单明细id（order_item.id）

    private Integer refundQuantity; //申请售后的商品数量

    private BigDecimal refundAmount; //申请退款金额

    private String reason;

    private Integer status; //售后单状态(0:待审核, 1:待寄回, 2:待收货, 3:退款中, 4:已完成, 5:已拒绝, 6:已取消)

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
