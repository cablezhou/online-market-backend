package com.zhoucable.marketbackend.modules.product.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductCreateDTO {

    @NotNull(message = "所属店铺ID不能为空")
    private Long storeId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    //分类ID可以先设为可选，后续再完善分类选择功能
    private Long category1Id;
    private Long category2Id;
    private Long category3Id;

    @NotEmpty(message = "至少选择一张主图")
    private String mainImage;

    private List<String> images; //轮播图url列表

    private List<Map<String, Object>> detailContent; //图文详情

    @Valid //嵌套校验SKU列表
    @NotEmpty(message = "至少选择一个SKU规格")
    private List<SkuDTO> skus;

    //status默认为1（上架），无需DTO传递
}
