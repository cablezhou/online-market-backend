package com.zhoucable.marketbackend.modules.product.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 请求商品详细信息时，其SKU列表的其中一项
 * @author 周开播
 * @Date 2025年10月24日14:26:03
 */
@Data
public class ProductSkuVO {
    private Long id; //SKU ID；
    private List<Map<String, String>> specifications; //规格列表，例如[{"key":"颜色","value":"红色"}, {"key":"尺码","value":"L"}]
    private BigDecimal price; //当前SKU价格
    private Integer stock; //当前SKU库存
    private String image; //当前SKU的独立图片
}
