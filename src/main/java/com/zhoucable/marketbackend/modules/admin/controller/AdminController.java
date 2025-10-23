package com.zhoucable.marketbackend.modules.admin.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.admin.dto.AdminReviewDTO;
import com.zhoucable.marketbackend.modules.admin.service.AdminService;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制类
 * @author 周开播
 * @Date 2025年10月23日10:12:13
 */
@RestController
@RequestMapping("api/admin/merchant")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/applications")
    public Result<Page<MerchantApplication>> getApplication(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status //用于查找对应状态的申请
    ){
        Page<MerchantApplication> pageInfo = new Page<>(page, size);
        //直接使用AdminService继承自IService的分页查询
        adminService.page(pageInfo,
                //构造查询条件
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantApplication>()
                        .eq(status != null, MerchantApplication::getStatus, status) // 如果status字段不为null，则添加status条件
                        .orderByDesc(MerchantApplication::getCreateTime)
                );
        return Result.success(pageInfo);
    }

    /**
     * 管理员审核申请（设计文档3.4.3）
     * @param id 申请的ID
     * @param reviewDTO 管理员的审核操作（前端传来）
     * @return 操作结果
     * @author 周开播
     * @Date 2025年10月23日10:22:10
     */
    @PutMapping("/application/{id}")
    public Result<Void> reviewApplication(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminReviewDTO reviewDTO
            ){

        adminService.reviewApplication(id,reviewDTO);
        return Result.success();
    }
}
