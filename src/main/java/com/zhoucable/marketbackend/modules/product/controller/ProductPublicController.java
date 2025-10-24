package com.zhoucable.marketbackend.modules.product.controller;


import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.product.dto.ProductDetailVO;
import com.zhoucable.marketbackend.modules.product.dto.ProductListQueryDTO;
import com.zhoucable.marketbackend.modules.product.dto.ProductListVO;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公用的商品接口（获取商品列表等）
 * @author 周开播
 * @Date 2025年10月24日14:11:06
 */
@RestController
@RequestMapping("/api/products") //公用接口的基础路径
public class ProductPublicController {

    @Autowired
    private ProductService productService;

    /**
     * 商品列表展示（FR-PM-001）
     * @param queryDTO 分页及筛选条件
     * @return 商品列表分页结果
     * @author 周开播
     * @Date 2025年10月24日14:14:54
     */
    @GetMapping
    public Result<PageResult<ProductListVO>> listProducts(ProductListQueryDTO queryDTO){

        PageResult<ProductListVO> pageResult = productService.listProducts(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 商品详情（FR-PM-002）
     * @param id 商品SPU id
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getProductDetails(@PathVariable Long id){
        ProductDetailVO detailVO = productService.getProductDetails(id);
        return Result.success(detailVO);
    }
}
