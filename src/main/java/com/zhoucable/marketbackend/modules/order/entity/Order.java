package com.zhoucable.marketbackend.modules.order.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.springframework.validation.ObjectError;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 子订单表-实体
 * @author 周开播
 * @Date 2025年10月29日10:08:39
 */
@Data
@TableName(value = "orders", autoResultMap = true) // 开启 autoResultMap 支持 TypeHandler
public class Order {

    private Long id; //子订单主键id，自增

    private Long parentOrderId; //关联的父订单id

    private Long storeId; //所属店铺id

    private String orderNumber; //子订单业务订单号，全局唯一，用于用户查找

    private Long userId; //下单用户id

    private BigDecimal totalAmount; //子订单总金额（商品总价）

    private Integer status; //订单状态（0：待支付，1：待发货，...，6：已退款）

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> addressSnapshot; //收货地址快照，JSON格式

    private String note; //用户下单备注

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> shippingInfo; //物流信息（快递公司，快递单号），JSON格式

    private LocalDateTime shippingTime; //商家发货时间

    private LocalDateTime completionTime; //订单完成（用户确认收货）时间

    private LocalDateTime cancelTime; //订单取消时间

    private LocalDateTime createTime; //记录创建时间

    private LocalDateTime updateTime; //记录更新时间

    private Integer previousStatus; //进入退款流程前的原状态

    @Version // 标记为乐观锁版本号字段
    private Integer version; //乐观锁版本号


}
