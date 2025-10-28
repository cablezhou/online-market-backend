package com.zhoucable.marketbackend.modules.shoppingcart.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车商品数量的DTO
 * @author 周开播
 * @Date 2025年10月28日14:21:45
 */
@Data
public class UpdateItemDTO {

    @NotNull(message = "修改数量不能为空")
    @Min(value = 1, message = "修改数量至少为1")
    private Integer quantity;
}
