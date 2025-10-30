package com.zhoucable.marketbackend.modules.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.order.dto.CreateOrderDTO;
import com.zhoucable.marketbackend.modules.order.dto.OrderListQueryDTO;
import com.zhoucable.marketbackend.modules.order.dto.ShipOrderDTO;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.vo.OrderDetailVO;
import com.zhoucable.marketbackend.modules.order.vo.OrderListVO;
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

    /**
     * 查询用户订单列表（分页）（FR-OM-002）
     * @param userId 用户id
     * @param queryDTO 查询条件（分页、订单状态）
     * @return 分页结果
     */
    PageResult<OrderListVO> getOrderList(Long userId, OrderListQueryDTO queryDTO);

    /**
     * 查询订单详情（FR-OM-003）
     * @param userId 用户id
     * @param orderNumber 子订单号
     * @return 订单详情VO
     */
    OrderDetailVO getOrderDetail(Long userId, String orderNumber);


    /**
     * 用户取消订单 (FR-OM-005)
     * @param userId 用户id
     * @param orderNumber 子订单号
     */
    void cancelOrder(Long userId, String orderNumber);


    /**
     * 商家发货（FR-OM-006）
     * @param merchantUserId 商家用户ID (用于权限校验)
     * @param orderNumber 要发货的子订单号
     * @param shipOrderDTO 物流信息
     */
    void shipOrder(Long merchantUserId, String orderNumber, ShipOrderDTO shipOrderDTO);
}
