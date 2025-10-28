package com.zhoucable.marketbackend.modules.shoppingcart.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加商品到购物车
 * @author 周开播
 * @Date 2025年10月28日10:53:51
 */
@Data
public class AddItemDTO {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "添加数量至少为1")
    private Integer quantity;
}
