package com.zhoucable.marketbackend.modules.order.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家发货DTO
 * @author 周开播
 * @Date 2025年10月30日14:27:51
 */
@Data
public class ShipOrderDTO {

    @NotBlank(message = "物流公司不能为空")
    private String shippingCompany;

    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;
}
