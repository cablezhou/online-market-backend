package com.zhoucable.marketbackend.modules.merchant.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.merchant.dto.MerchantApplyDTO;
import com.zhoucable.marketbackend.modules.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家相关的服务层
 * @author 周开播
 * @Date 2025/10/22 16:57
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    /**
     * 用户提交成为商家的申请
     * @param applyDTO 申请信息DTO
     * @return 操作结果（不是审核结果）
     */
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody MerchantApplyDTO applyDTO){
        merchantService.apply(applyDTO);
        return Result.success();
    }
}
