package com.zhoucable.marketbackend.modules.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 修改商品信息时，SKU部分的DTO（包含在ProductUpdateDTO中）
 * 在SkuDTO基础上新增id字段，用于识别哪个是已存在的SKU需要更新，若id为0或null，则表明是新增SKU
 * @author 周开播
 * @Date 2025年10月24日16:12:40
 */
@Data
public class SkuUpdateDTO {

    //新增的字段
    private Long id; //SKU ID，如果为null或0，表明是新增SKU，否则只是更新

    @Valid
    @NotEmpty(message = "SKU规格不能为空")
    private List<SpecificationDTO> specifications;

    @NotNull(message = "SKU价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "SKU库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String skuCode;
    private String image;

    // TODO：可以考虑增加 status 字段，允许商家单独禁用某个 SKU（也可以不加，禁用SKU将库存设为0即可）
    // private Integer status;
}
