package com.zhoucable.marketbackend.modules.refund.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.refund.dto.RefundApplyDTO;
import com.zhoucable.marketbackend.modules.refund.service.RefundOrderService;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 售后控制器
 * @author 周开播
 * @Date 2025年10月31日15:46:33
 */
@RestController
@RequestMapping("/api/refund")
public class RefundController {

    @Autowired
    private RefundOrderService refundOrderService;

    @PostMapping("/apply")
    public Result<Void> applyForRefund(@Valid @RequestBody RefundApplyDTO applyDTO){
        Long userId = BaseContext.getCurrentId();
        refundOrderService.applyForRefund(userId, applyDTO);
        return Result.success();
    }

    // TODO: 未来可在此处添加“用户查询售后列表”、“用户取消售后”、“用户填写退货物流”等接口
}
