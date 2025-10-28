package com.zhoucable.marketbackend.modules.shoppingcart.vo;


import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车单个商品的视图对象
 */
@Data
public class CartItemVO {

    private Long cartItemId; //购物车项id

    private Long skuId;

    private Long productId;

    private Long storeId;

    private String productName;

    private String storeName;

    private String image; //优先使用SKU的image，若无则使用Product的mainImage

    private String specifications; //规格描述，例如“颜色：红色；尺码：L”

    private BigDecimal price; //当前SKU价格

    private Integer quantity; //购物车中的数量

    private Integer stock; //SKU的实时库存

    //数据库辅助字段，用于转换
    private String skuImage;
    private String mainImage;
    private String skuSpecifications; //数据库查出的JSON规格

    /**
     * 格式化规格
     * 将 "[{\"key\":\"颜色\",\"value\":\"红色\"},{\"key\":\"尺码\",\"value\":\"L\"}]"
     * 转换为 "颜色:红色; 尺码:L"
     */
    public String getSpecifications(){
        if(skuSpecifications == null || skuSpecifications.isEmpty()){
            return "";
        }
        // 复用 ProductSkuServiceImpl 中的逻辑，反向解析
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> specs = objectMapper.readValue(skuSpecifications, new TypeReference<>() {});
            return specs.stream()
                    .map(spec -> spec.get("key") + ":" + spec.get("value"))
                    .collect(Collectors.joining("; "));
        } catch (JsonProcessingException e) {
            return "规格解析错误";
        }
    }

    /**
     * 决定显示哪张图片
     * 优先SKU图片，其次SPU主图
     */
    public String getImage() {
        return (skuImage != null && !skuImage.isEmpty()) ? skuImage : mainImage;
    }
}
