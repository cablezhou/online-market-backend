package com.zhoucable.marketbackend.modules.pay.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentStatusVO;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentSubmitVO;
import com.zhoucable.marketbackend.modules.pay.dto.PaymentSubmitDTO;
import com.zhoucable.marketbackend.modules.pay.service.PaymentService;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 * @author 周开播
 * @Date 2025年10月31日16:24:52
 */
@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;


    /**
     * 用户发起支付（FR-PAY-001）
     * @param paymentSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    public Result<PaymentSubmitVO> submitPayment(@Valid @RequestBody PaymentSubmitDTO paymentSubmitDTO){
        Long userId = BaseContext.getCurrentId();
        PaymentSubmitVO vo = paymentService.submitPayment(userId, paymentSubmitDTO);
        return Result.success(vo);
    }

    /**
     * 用户主动查询支付状态（FR-PAY-003）
     * @param parentOrderNumber 父订单号
     * @return
     * @Date 2025年11月3日10:24:03
     */
    @GetMapping("/status/{parentOrderNumber}")
    public Result<PaymentStatusVO> queryPaymentStatus(@PathVariable String parentOrderNumber){
        Long userId = BaseContext.getCurrentId();
        PaymentStatusVO vo = paymentService.queryPaymentStatus(userId, parentOrderNumber);
        return Result.success(vo);
    }
}
