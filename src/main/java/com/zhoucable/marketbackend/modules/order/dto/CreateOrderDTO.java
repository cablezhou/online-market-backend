package com.zhoucable.marketbackend.modules.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单的DTO（从前端接收）
 * @author 周开播
 * @Date 2025年10月29日10:30:13
 */
@Data
public class CreateOrderDTO {

    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartItemIds; //待结算的购物车项id

    @NotNull(message = "请选择收货地址")
    private Long addressId; //收货地址id

    //可选：用户备注
    private String note;
}
