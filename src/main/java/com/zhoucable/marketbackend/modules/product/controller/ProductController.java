package com.zhoucable.marketbackend.modules.product.controller;


import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.product.dto.*;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商品控制类
 * @author 周开播
 * @Date 2025年10月23日16:47:51
 */
@RestController
// 注意区分商家接口和通用接口的路径
// @RequestMapping("/api/products") // 通用接口前缀 (用于列表、详情等)
// @RequestMapping("/api/admin/products") // 管理员接口前缀
@RequestMapping("/api/merchant/products")
public class ProductController {

    @Autowired
    private ProductService productService;


    /**
     * 商家创建新商品（FR-PM-003）
     * @param createDTO 商品及SKU信息
     * @return 结果
     */
    @PostMapping
    public Result<Product> createProduct(@Valid @RequestBody ProductCreateDTO createDTO){
        Product createdProduct = productService.createProduct(createDTO);
        return Result.success(createdProduct);
    }

    /**
     * 商品列表展示（FR-PM-001）
     * @param queryDTO 分页及筛选条件
     * @return 商品列表分页结果
     * @author 周开播
     * @Date 2025年10月24日11:30:53
     */
    @GetMapping
    private Result<PageResult<ProductListVO>> listProducts(ProductListQueryDTO queryDTO){
        //Spring MVC会自动将query参数绑定到DTO对象
        PageResult<ProductListVO> pageResult = productService.listProducts(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 商家修改商品状态（上架下架）（FR-PM-005)
     * @param id 商品 SPU ID
     * @param updateStatusDTO 新的状态信息
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusDTO updateStatusDTO
            ){
        productService.updateProductStatus(id, updateStatusDTO);
        return Result.success();
    }

    /**
     * 商家修改商品信息 (FR-PM-004)
     * @param id 商品 SPU ID
     * @param updateDTO 商品更新信息 (包含 SPU 和 SKU)
     * @return 操作结果
     * @Date 2025年10月24日17:06:55
     */
    @PutMapping("/{id}")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDTO updateDTO
            ){
        productService.updateProduct(id,updateDTO);
        return Result.success();
    }

    // --- 未来添加其他接口 ---
    // @PutMapping("/{id}/status") // 商家修改状态
    // @GetMapping // 通用-商品列表 (需要移到非 /merchant 前缀下)
    // @GetMapping("/{id}") // 通用-商品详情 (需要移到非 /merchant 前缀下)

}
