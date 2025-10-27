package com.zhoucable.marketbackend.modules.product.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员强制修改商品上下架状态DTO
 * 相比商家自行上下架的DTO，新增了reason字段
 * @author 周开播
 * @Date 2025年10月27日14:12:54
 */
@Data
public class AdminUpdateStatusDTO {

    @NotNull(message = "状态值不能为空")
    @Min(value = 0, message = "状态值必须为0（下架）或1（上架）")
    @Max(value = 1, message = "状态值必须为0（下架）或1（上架）")
    private Integer status;

    private String reason; //具体是否填写可选
}
