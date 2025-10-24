package com.zhoucable.marketbackend.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一项具体规格规格DTO
 * 示例：{"key":"颜色","value":"红色"}
 * @author 周开播
 * @Date 2025年10月23日16:03:23
 */
@Data
@EqualsAndHashCode //便于后续排序和比较
public class SpecificationDTO {

    @NotBlank(message = "规格名不能为空")
    private String key;

    @NotBlank(message = "规格值不能为空")
    private String value;
}
