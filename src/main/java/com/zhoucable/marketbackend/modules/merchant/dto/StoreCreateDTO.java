package com.zhoucable.marketbackend.modules.merchant.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 商家主动创建新店铺（除申请时默认创建的店铺外）
 * @author 周开播
 * @Date 2025年10月23日11:21:40
 */
@Data
public class StoreCreateDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Length(max = 100, message = "店铺名称不能超过100个字符")
    private String name;

    @Length(max = 500, message = "店铺简介不能超过500个字符")
    private String description;
}
