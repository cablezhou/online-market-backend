package com.zhoucable.marketbackend.modules.pay.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.pay.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟支付控制器
 * 仅用于开发测试
 * @author 周开播
 * @Date 2025年10月31日16:46:23
 */
@RestController
@RequestMapping("/api/mock/pay")
public class MockPaymentController {
    @Autowired
    private PaymentService paymentService;

    /**
     * 模拟支付成功的回调
     * 用户在前端点击模拟支付URL，会跳转到此接口
     * @param orderNumber 订单号
     * @return
     */
    @GetMapping("/success")
    public Result<String> mockPaySuccess(
            @RequestParam String orderNumber,
            @RequestParam Integer paymentMethod
    ) {
        // 模拟一个第三方流水号
        String mockTransactionId = "MOCK_TRANS_" + System.currentTimeMillis();

        // 调用核心支付成功逻辑
        paymentService.processSuccessfulPayment(orderNumber, paymentMethod, mockTransactionId);

        // 返回一个对用户友好的提示
        return Result.success("支付成功！订单号：" + orderNumber);
    }
}
