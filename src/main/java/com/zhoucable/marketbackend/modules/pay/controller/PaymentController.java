package com.zhoucable.marketbackend.modules.pay.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentSubmitVO;
import com.zhoucable.marketbackend.modules.pay.dto.PaymentSubmitDTO;
import com.zhoucable.marketbackend.modules.pay.service.PaymentService;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/submit")
    public Result<PaymentSubmitVO> submitPayment(@Valid @RequestBody PaymentSubmitDTO paymentSubmitDTO){
        Long userId = BaseContext.getCurrentId();
        PaymentSubmitVO vo = paymentService.submitPayment(userId, paymentSubmitDTO);
        return Result.success(vo);
    }
}
