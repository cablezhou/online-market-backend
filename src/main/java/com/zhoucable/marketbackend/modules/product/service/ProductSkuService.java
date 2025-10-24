package com.zhoucable.marketbackend.modules.product.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.product.dto.SpecificationDTO;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;

import java.util.List;

/**
 * 处理SKU相关逻辑，包括规格标准化和查重
 * @author 周开播
 * @Date 2025年10月23日16:14:23
 */
public interface ProductSkuService extends IService<ProductSku> {

    /**
     * 标准化规格JSON（按 key 排序并转为紧凑字符串）
     * @param specifications 规格DTO列表
     * @return 标准化后的JSON字符串
     */
    String standardizeSpecifications(List<SpecificationDTO> specifications);

    /**
     * 检查指定商品下是否存在具有相同规格的SKU
     * @param productId 商品ID
     * @param standardizedSpecJson 标准化后的规格JSON字符串
     * @return 是否存在重复
     */
    boolean checkDuplicateSku(Long productId, String standardizedSpecJson);

}
