package com.zhoucable.marketbackend.modules.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.entity.ParentOrder;
import com.zhoucable.marketbackend.modules.order.mapper.OrderMapper;
import com.zhoucable.marketbackend.modules.order.mapper.ParentOrderMapper;
import com.zhoucable.marketbackend.modules.pay.VO.PaymentSubmitVO;
import com.zhoucable.marketbackend.modules.pay.dto.PaymentStatus;
import com.zhoucable.marketbackend.modules.pay.dto.PaymentSubmitDTO;
import com.zhoucable.marketbackend.modules.pay.entity.PaymentTransaction;
import com.zhoucable.marketbackend.modules.pay.mapper.PaymentTransactionMapper;
import com.zhoucable.marketbackend.modules.pay.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 支付服务实现
 * @author 周开播
 * @Date 2025年10月31日16:10:56
 */
@Service
@Slf4j
public class PaymentServiceImpl extends ServiceImpl<PaymentTransactionMapper, PaymentTransaction> implements PaymentService {

    @Autowired
    private ParentOrderMapper parentOrderMapper;

    @Autowired
    private PaymentTransactionMapper paymentTransactionMapper;

    @Autowired
    private OrderMapper orderMapper;


    @Override
    @Transactional
    public PaymentSubmitVO submitPayment(Long userId, PaymentSubmitDTO paymentSubmitDTO){

        //1.根据OrderNumber查询父订单（ParentOrder）
        LambdaQueryWrapper<ParentOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ParentOrder::getParentOrderNumber, paymentSubmitDTO.getOrderNumber());
        ParentOrder parentOrder = parentOrderMapper.selectOne(queryWrapper);

        //2.校验订单
        if(parentOrder == null){
            throw new BusinessException(404, "订单不存在");
        }
        if(!Objects.equals(parentOrder.getUserId(), userId)){
            throw new BusinessException(403, "无权支付该订单");
        }
        if(parentOrder.getPaymentTime() != null){
            throw new BusinessException(409, "订单已支付，请勿重复操作");
        }

        //3.创建支付流水
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderNumber(parentOrder.getParentOrderNumber());
        transaction.setAmount(parentOrder.getTotalAmount()); //使用父订单的总金额
        transaction.setStatus(PaymentStatus.PENDING.getCode()); //0：处理中
        transaction.setPaymentMethod(paymentSubmitDTO.getPaymentMethod());
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());

        paymentTransactionMapper.insert(transaction);
        log.info("创建支付流水成功，流水ID: {}, 订单号: {}", transaction.getId(), transaction.getOrderNumber());

        //4.调用支付渠道并组装返回VO
        //TODO:调用真实支付渠道（微信、支付宝SDK）
        log.info("TODO: 模拟调用第三方支付接口...");

        //5.组装VO
        PaymentSubmitVO vo = new PaymentSubmitVO();
        // (加入真实支付逻辑后在这里填充 prepayId, nonceStr 等)
        // 先返回一个模拟支付URL，方便前端测试
        vo.setMockPayUrl("http://localhost:8080/api/mock/pay/success?orderNumber=" + parentOrder.getParentOrderNumber());

        return vo;
    }


    @Override
    @Transactional
    public void processSuccessfulPayment(String parentOrderNumber, Integer paymentMethod, String transactionId){
        log.info("开始处理支付成功回调，父订单号: {}", parentOrderNumber);

        //1.查找父订单
        LambdaQueryWrapper<ParentOrder> poWrapper = new LambdaQueryWrapper<>();
        poWrapper.eq(ParentOrder::getParentOrderNumber, parentOrderNumber);
        ParentOrder parentOrder = parentOrderMapper.selectOne(poWrapper);

        if (parentOrder == null) {
            log.error("支付回调失败：未找到父订单 {}", parentOrderNumber);
            throw new BusinessException(404, "订单不存在");
        }

        //关键：幂等性处理
        // 检查父订单的支付时间是否已填写
        if (parentOrder.getPaymentTime() != null) {
            log.warn("支付回调重复通知：父订单 {} 已处理。", parentOrderNumber);
            return; // 已处理，直接返回成功，不再重复执行
        }

        LocalDateTime paymentTime = LocalDateTime.now();

        //3.更新支付流水
        LambdaUpdateWrapper<PaymentTransaction> transUpdateWrapper = new LambdaUpdateWrapper<>();
        transUpdateWrapper.eq(PaymentTransaction::getOrderNumber, parentOrderNumber)
                .eq(PaymentTransaction::getStatus, PaymentStatus.PENDING.getCode()) // 仅更新“处理中”的流水
                .set(PaymentTransaction::getStatus, PaymentStatus.SUCCESS.getCode())
                .set(PaymentTransaction::getTransactionId, transactionId) //第三方支付流水号
                .set(PaymentTransaction::getUpdateTime, paymentTime);
        int transRows = paymentTransactionMapper.update(transUpdateWrapper);
        if(transRows == 0){
            log.error("支付回调严重异常：未找到匹配的支付流水或流水状态不正确。订单号: {}", parentOrderNumber);
            // 必须抛出异常，让事务回滚
            throw new BusinessException(500, "支付流水状态异常");
        }
        log.info("支付流水更新成功，订单号: {}", parentOrderNumber);

        //4.更新父订单
        parentOrder.setPaymentAmount(parentOrder.getPaymentAmount()); //暂定实付=总价
        parentOrder.setPaymentMethod(paymentMethod);
        parentOrder.setPaymentTime(paymentTime);
        parentOrder.setUpdateTime(paymentTime);
        parentOrderMapper.updateById(parentOrder);
        log.info("父订单更新成功, ID: {}", parentOrder.getId());

        //5.更新所有子订单
        // 将所有关联的子订单状态从 0 (待支付) -> 1 (待发货)
        LambdaUpdateWrapper<Order> orderUpdateWrapper = new LambdaUpdateWrapper<>();
        orderUpdateWrapper.eq(Order::getParentOrderId, parentOrder.getId())
                .eq(Order::getStatus, 0) //待支付
                .set(Order::getStatus, 1) //更新为待发货
                .set(Order::getUpdateTime, paymentTime);

        int orderRows = orderMapper.update(orderUpdateWrapper);
        log.info("子订单状态更新成功, 父订单ID: {}, 共更新 {} 条子订单。", parentOrder.getId(), orderRows);

        // 6. TODO: 发送异步消息（MQ），通知商家“新订单”
        log.info("TODO: 订单 {} 已支付，发送MQ通知商家...", parentOrderNumber);

    }
}
