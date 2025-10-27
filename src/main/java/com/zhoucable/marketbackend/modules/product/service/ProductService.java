package com.zhoucable.marketbackend.modules.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.product.dto.*;
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

    /**
     * 获取商品列表（分页）-公共接口
     * @param queryDTO 查询条件及分页参数
     * @return 分页结果
     */
    PageResult<ProductListVO> listProducts(ProductListQueryDTO queryDTO);

    /**
     * 商家获取其店铺的商品列表
     * @param storeId 商家店铺id
     * @param queryDTO 查询条件（name，status）及分页参数
     * @return 分页结果
     * @Date 2025年10月27日15:34:17
     */
    PageResult<ProductListVO> listMerchantProducts(Long storeId, MerchantProductListQueryDTO queryDTO);

    /**
     * 获取商品详情（SPU+SKU列表）
     * @param productId 商铺SPU ID
     * @return 商品详情VO
     * @author 周开播
     * @Date 2025年10月24日14:32:45
     */
    ProductDetailVO getProductDetails(Long productId);

    /**
     * 商家更新商品状态（上架、下架）
     * @param productId 商品SPU id
     * @param updateStatusDTO 新的状态信息
     */
    void updateProductStatus(Long productId, UpdateStatusDTO updateStatusDTO);

    /**
     * 商家修改商品信息（SPU + SKUs）
     * @param productId 要修改的商品SPU id
     * @param updateDTO 更新信息
     * @Date 2025年10月24日16:21:15
     */
    void updateProduct(Long productId, ProductUpdateDTO updateDTO);
}
