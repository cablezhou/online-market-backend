package com.zhoucable.marketbackend.modules.review.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品评价展示VO
 * FR-PR-002
 * @author 周开播
 * @Date 2025年11月3日14:34:52
 */
@Data
@Slf4j
public class ReviewVO {

    private String nickname; //评价用户的呢称
    private String avatar; //评价用户的头像（User表暂未创建此字段，先预留）

    //评价内容
    private Integer rating;
    private String content;
    private List<String> images;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //增加显示该用户购买的规格
    private String specifications; //例如：“颜色：红色；尺码：L”

    /**
     * 数据库辅助字段，用于接受order_item表中的sku_snapshot（JSON字符串）
     * 使用jsonIgnore确保不会被序列化返回前端
     */
    @JsonIgnore
    private String skuSnapshotJson;

    /**
     * 重写 getSpecifications 方法，动态解析JSON
     * (逻辑复用自 CartItemVO)
     */
    public String getSpecifications() {
        if (skuSnapshotJson == null || skuSnapshotJson.isEmpty()) {
            return "";
        }

        // (此处每次都 new ObjectMapper 效率较低，但对于VO层是可接受的简单做法)
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // skuSnapshot 存储的是 List<Map<String, String>>
            Object specsObj = objectMapper.readValue(skuSnapshotJson, Map.class).get("specifications");

            // 转换规格
            List<Map<String, String>> specs = objectMapper.convertValue(specsObj, new TypeReference<>() {});

            return specs.stream()
                    .map(spec -> spec.get("key") + ":" + spec.get("value"))
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            log.warn("解析SKU快照规格失败 (来自ReviewVO): {}", e.getMessage());
            return "规格解析错误";
        }
    }
}
