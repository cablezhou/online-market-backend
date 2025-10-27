package com.zhoucable.marketbackend.modules.product.dto;

import com.zhoucable.marketbackend.common.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家查询其店铺商品列表的DTO (FR-PM-008)
 * @author 周开播
 * @Date 2025年10月27日15:32:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantProductListQueryDTO extends PageQueryDTO {

    /**
     * 按商品名称模糊查询 (可选)
     */
    private String name;

    /**
     * 按商品状态筛选 (可选, 1:上架, 0:下架)
     */
    private Integer status;

    // storeId 将从路径参数获取，不由DTO传入
}