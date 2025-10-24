package com.zhoucable.marketbackend.modules.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.product.dto.ProductCreateDTO;
import com.zhoucable.marketbackend.modules.product.entity.Product;

/**
 * 处理商品SPU和SKU的整体创建逻辑
 * @author 周开播
 * @Date 2025年10月23日16:25:44
 */
public interface ProductService extends IService<Product> {

    /**
     * 商家创建新商品（SPU + SKUs）
     * @param createDTO 商品创建信息
     * @return 创建的Product对象（包含生成的ID）
     */
    Product createProduct(ProductCreateDTO createDTO);
}
