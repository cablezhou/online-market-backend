package com.zhoucable.marketbackend.modules.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家更改商品上下架状态的DTO
 * @author 周开播
 * @Date 2025年10月24日14:58:29
 */
@Data
public class UpdateStatusDTO {

    @NotNull(message = "状态不能为空")
    //添加校验，确保status只能是0或1
    @Min(value = 0, message = "状态值必须为 0（下架）或 1（上架）")
    @Max(value = 1, message = "状态值必须为 0（下架）或 1（上架）")
    private Integer status;
}
