package com.zhoucable.marketbackend.modules.product.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 商品详细信息视图
 * @author 周开播
 * @Date 2025年10月24日14:28:15
 */
@Data
public class ProductDetailVO {

    // ----SPU信息----
    private Long id;
    private Long storeId; //可以考虑返回店铺名称而非ID
    private String name;
    private List<Map<String, Object>> detailContent; //图文详情
    private Integer sales;
    private String mainImage;
    private List<String> images; //轮播图

    //----SKU列表----
    private List<ProductSkuVO> skus; //包含在售的SKU规格、价格、库存等

}
