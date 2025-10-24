package com.zhoucable.marketbackend.modules.product.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建SKU时的数据传输对象（在createDTO的列表中）
 * 一个SkuDTO代表一个具体规格，有其对应的唯一库存、价格
 * @author 周开播
 * @Date 2025年10月23日16:04:51
 */
@Data
public class SkuDTO {

    @Valid
    @NotEmpty(message = "SKU规格不能为空")
    private List<SpecificationDTO> specifications; //一种具体的、唯一的规格组合，例如：{"key":"颜色","value":"红色"}, {"key":"尺码","value":"L"}

    @NotNull(message = "SKU价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "SKU库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String skuCode; //可选
    private String image; //可选，SKU独立图片
}
