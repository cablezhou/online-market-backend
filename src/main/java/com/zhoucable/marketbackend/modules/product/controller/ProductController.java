package com.zhoucable.marketbackend.modules.product.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.product.dto.ProductCreateDTO;
import com.zhoucable.marketbackend.modules.product.entity.Product;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // --- 未来添加其他接口 ---
    // @PutMapping("/{id}") // 商家修改商品
    // @PutMapping("/{id}/status") // 商家修改状态
    // @GetMapping // 通用-商品列表 (需要移到非 /merchant 前缀下)
    // @GetMapping("/{id}") // 通用-商品详情 (需要移到非 /merchant 前缀下)

}
