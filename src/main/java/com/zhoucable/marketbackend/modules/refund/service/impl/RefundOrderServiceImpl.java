package com.zhoucable.marketbackend.modules.refund.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.entity.OrderItem;
import com.zhoucable.marketbackend.modules.order.mapper.OrderMapper;
import com.zhoucable.marketbackend.modules.refund.dto.RefundApplyDTO;
import com.zhoucable.marketbackend.modules.refund.dto.RefundReviewDTO;
import com.zhoucable.marketbackend.modules.refund.entity.RefundOrder;
import com.zhoucable.marketbackend.modules.refund.mapper.RefundOrderMapper;
import com.zhoucable.marketbackend.modules.refund.service.RefundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;import com.zhoucable.marketbackend.modules.order.mapper.OrderItemMapper;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class RefundOrderServiceImpl extends ServiceImpl<RefundOrderMapper, RefundOrder> implements RefundOrderService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StoreService storeService;

    //售后单号生成格式
    private static final DateTimeFormatter REFUND_NUMBER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成唯一售后单号
     */
    private String generateRefundOrderNumber(){
        // 格式：R + yyyyMMddHHmmssSSS + 4位随机数
        String timeStamp = LocalDateTime.now().format(REFUND_NUMBER_FORMATTER);
        String randomDigits = String.format("%04d", ThreadLocalRandom.current().nextInt(10000)); //0000-9999
        return "R" + timeStamp + randomDigits;
    }

    @Override
    @Transactional
    public void applyForRefund(Long userId, RefundApplyDTO applyDTO){

        //1.校验订单项
        OrderItem orderItem = orderItemMapper.selectById(applyDTO.getOrderItemId());
        if(orderItem == null){
            throw new BusinessException(404,"订单项不存在");
        }

        //2.校验订单归属
        Order order = orderMapper.selectById(orderItem.getOrderId());
        if(order == null || !Objects.equals(order.getUserId(), userId)){
            throw new BusinessException(403, "无权操作该订单");
        }

        //3.校验订单状态（必须是已支付、已发货、已完成）
        Integer orderStatus = order.getStatus();
        if(orderStatus != 1 && orderStatus != 2 && orderStatus != 3){
            throw new BusinessException(409, "订单当前状态无法申请售后");
        }

        //4.校验售后状态和数量
        if(applyDTO.getQuantity() <= 0){
            throw new BusinessException(400, "申请数量必须大于0");
        }

        int remainingQuantity = orderItem.getQuantity() - orderItem.getRefundedQuantity();
        if(applyDTO.getQuantity() > remainingQuantity){
            throw new BusinessException(409, "申请数量超过可售后数量，剩余可申请 " + remainingQuantity + " 件");
        }

        //5.计算退款金额
        //（单价 * 申请数量）
        BigDecimal refundAmount = orderItem.getPriceSnapshot().multiply(BigDecimal.valueOf(applyDTO.getQuantity()));

        //6.更新订单状态
        // set refund_status = 1 (售后中), refunded_quantity = refunded_quantity + ?
        LambdaUpdateWrapper<OrderItem> itemUpdateWrapper = new LambdaUpdateWrapper<>();
        itemUpdateWrapper.eq(OrderItem::getId, orderItem.getId())
                .eq(OrderItem::getRefundedQuantity, orderItem.getRefundedQuantity()) //乐观锁
                .setSql("refund_status = 1, refunded_quantity = refunded_quantity + " + applyDTO.getQuantity());
        int affectedRows = orderItemMapper.update(itemUpdateWrapper);
        if(affectedRows == 0){
            log.warn("更新订单项售后状态失败，可能存在并发申请。OrderItemId: {}", orderItem.getId());
            throw new BusinessException(500, "操作失败，请重试");
        }

        //7.创建售后单
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundOrderNumber(generateRefundOrderNumber());
        refundOrder.setUserId(userId);
        refundOrder.setStoreId(order.getStoreId());
        refundOrder.setOrderId(order.getId());
        refundOrder.setOrderItemId(orderItem.getId());
        refundOrder.setRefundQuantity(applyDTO.getQuantity());
        refundOrder.setRefundAmount(refundAmount);
        refundOrder.setReason(applyDTO.getReason());
        refundOrder.setStatus(0); // 0:待审核
        refundOrder.setCreateTime(LocalDateTime.now());
        refundOrder.setUpdateTime(LocalDateTime.now());

        this.save(refundOrder);

        // TODO: 通知商家有新的售后申请
        log.info("用户 {} 为订单项 {} 提交售后申请 {} 成功。", userId, orderItem.getId(), refundOrder.getRefundOrderNumber());

    }

    @Override
    @Transactional
    public void reviewRefund(Long merchantUserId, Long refundOrderId, RefundReviewDTO reviewDTO){

        //1.校验售后单
        RefundOrder refundOrder = this.getById(refundOrderId);
        if (refundOrder == null) {
            throw new BusinessException(404, "售后单不存在");
        }

        //2.校验商家权限
        Store store = storeService.getById(refundOrder.getStoreId());
        if (store == null || !Objects.equals(store.getUserId(), merchantUserId)) {
            throw new BusinessException(403, "无权操作该售后单");
        }

        //3.校验售后单状态
        if(!Objects.equals(refundOrder.getStatus(), 0)){ // 必须是 0:待审核
            throw new BusinessException(409, "售后单状态不正确，无法审核");
        }

        //4.获取关联的订单项和订单
        OrderItem orderItem = orderItemMapper.selectById(refundOrder.getOrderItemId());
        Order order = orderMapper.selectById(refundOrder.getOrderId());
        if (orderItem == null || order == null) {
            throw new BusinessException(500, "售后单关联数据异常");
        }

        if(RefundReviewDTO.ReviewAction.REJECT.equals(reviewDTO.getAction())){
            //5. --- 商家拒绝 ---
            if (reviewDTO.getReason() == null || reviewDTO.getReason().isBlank()) {
                throw new BusinessException(400, "拒绝退款必须填写原因");
            }

            //5.1更新售后单
            refundOrder.setStatus(5); //已拒绝
            refundOrder.setUpdateTime(LocalDateTime.now());
            // TODO:(可以增加一个 reason 字段来存储商家拒绝原因)
            this.updateById(refundOrder);

            //5.2回滚订单项状态
            LambdaUpdateWrapper<OrderItem> itemUpdateWrapper = new LambdaUpdateWrapper<>();
            itemUpdateWrapper.eq(OrderItem::getId, orderItem.getId())
                    .setSql("refund_status = 0, refunded_quantity = refunded_quantity - " + refundOrder.getRefundQuantity());
            orderItemMapper.update(itemUpdateWrapper);

            log.info("商家 {} 拒绝了售后单 {}", merchantUserId, refundOrderId);

        }else if(RefundReviewDTO.ReviewAction.APPROVE.equals(reviewDTO.getAction())){
            // 6. --- 商家同意 ---

            // 根据原订单状态，决定是“直接退款”还是“等待寄回”
            Integer orderStatus = order.getStatus();

            if(Objects.equals(orderStatus, 1)){
                // 6.1 场景一：原订单“待发货”。商家同意 -> 直接退款
                refundOrder.setStatus(3); //退款中

                // TODO: 在这里调用支付模块的退款接口
                log.info("TODO: 订单(待发货) 售后单 {} 审核通过，调用支付服务执行退款...", refundOrderId);
                // paymentService.executeRefund(refundOrder);
                // 退款应是异步的，先标记为退款中
            }else{
                // 6.2 场景二：原订单“已发货”或“已完成”。商家同意 -> 等待用户寄回
                refundOrder.setStatus(1); //待寄回
            }

            refundOrder.setUpdateTime(LocalDateTime.now());
            this.updateById(refundOrder);
            log.info("商家 {} 同意了售后单 {}，新状态为 {}", merchantUserId, refundOrderId, refundOrder.getStatus());
        }

        // TODO: 通知用户审核结果
    }
}
