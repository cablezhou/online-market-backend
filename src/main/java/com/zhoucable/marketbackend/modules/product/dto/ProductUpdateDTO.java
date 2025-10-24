package com.zhoucable.marketbackend.modules.product.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 前端商家修改商品信息传来的DTO
 * 不包括id等归属信息
 * 只包含可修改的信息
 * @author 周开播
 * @Date 2025年10月24日16:06:51
 */
@Data
public class ProductUpdateDTO {

    //SPU相关字段（允许更新）
    private String name;
    private Long category1Id;
    private Long category2Id;
    private Long category3Id;

    @NotEmpty(message = "至少需要一张主图")
    private String mainImage;

    private List<String> images;

    private List<Map<String, Object>> detailContent;

    //SKU列表（可能包含新增、修改、删除）
    @Valid
    @NotEmpty(message = "至少需要一个SKU规格")
    private List<SkuUpdateDTO> skus;
}
