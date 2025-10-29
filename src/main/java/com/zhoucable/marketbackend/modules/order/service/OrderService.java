package com.zhoucable.marketbackend.modules.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.order.dto.CreateOrderDTO;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.vo.OrderSubmitVO;

/**
 * 订单服务接口（子订单）
 * @author 周开播
 * @Date 2025年10月29日10:39:40
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单（FR-OM-001)
     * @param userId 下单用户id
     * @param createOrderDTO 订单创建信息DTO
     * @return 订单提交结果VO
     */
    OrderSubmitVO createOrder(Long userId, CreateOrderDTO createOrderDTO);
}
