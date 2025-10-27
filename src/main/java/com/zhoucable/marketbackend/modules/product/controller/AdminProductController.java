package com.zhoucable.marketbackend.modules.product.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.product.dto.AdminUpdateStatusDTO;
import com.zhoucable.marketbackend.modules.product.service.AdminProductService;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 管理员-商品控制类
 * @author 周开播
 * @Date 2025年10月27日14:31:11
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    @Autowired
    private AdminProductService adminProductService;

    @PutMapping("/products/{id}/status")
    public Result<Void> adminUpdateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateStatusDTO updateDTO
            ){
        adminProductService.adminUpdateProductStatus(id, updateDTO);
        return Result.success();
    }
}
