package com.zhoucable.marketbackend.modules.product.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.product.dto.AdminUpdateStatusDTO;
import com.zhoucable.marketbackend.modules.product.entity.Product;

/**
 * 管理员对商品的操作（强制下架等）
 * @author 周开播
 * @Date 2025年10月27日14:16:05
 */
public interface AdminProductService extends IService<Product> {

    /**
     * 管理员更新商品状态（FR-PM_007)
     * @param productId 商品 SPU id
     * @param updateDTO 管理员更新状态DTO
     */
    void adminUpdateProductStatus(Long productId, AdminUpdateStatusDTO updateDTO);
}
