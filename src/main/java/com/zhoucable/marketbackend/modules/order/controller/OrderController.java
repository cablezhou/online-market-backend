package com.zhoucable.marketbackend.modules.order.controller;

import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.order.dto.CreateOrderDTO;
import com.zhoucable.marketbackend.modules.order.dto.OrderListQueryDTO;
import com.zhoucable.marketbackend.modules.order.service.OrderService;
import com.zhoucable.marketbackend.modules.order.vo.OrderDetailVO;
import com.zhoucable.marketbackend.modules.order.vo.OrderListVO;
import com.zhoucable.marketbackend.modules.order.vo.OrderSubmitVO;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 * @author 周开播
 * @Date 2025年10月29日16:10:32
 */
@RestController
@RequestMapping("/api/orders") //用户端订单接口基础路径
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户创建订单（FR-OM-001）
     * @param createOrderDTO 包含购物车项ID和地址ID的DTO
     * @return 订单提交结果，包含父订单号和总金额
     */
    @PostMapping
    public Result<OrderSubmitVO> createOrder(@Valid @RequestBody CreateOrderDTO createOrderDTO){
        Long userId = BaseContext.getCurrentId();
        OrderSubmitVO submitVO = orderService.createOrder(userId,createOrderDTO);
        return Result.success(submitVO);
    }


    /**
     * 查询用户订单列表 (FR-OM-002)
     * @param queryDTO 分页参数 (page, size) 和可选的状态 (status)
     * @return 订单列表分页结果
     */
    @GetMapping
    public Result<PageResult<OrderListVO>> getOrderList(OrderListQueryDTO queryDTO){
        Long userId = BaseContext.getCurrentId();
        PageResult<OrderListVO> pageResult = orderService.getOrderList(userId, queryDTO);
        return Result.success(pageResult);
    }


    /**
     * 查询订单详情 (FR-OM-003)
     * @param orderNumber 子订单号 (从路径中获取)
     * @return 订单详情 VO
     */
    @GetMapping("/{orderNumber}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable String orderNumber){
        Long userId = BaseContext.getCurrentId();
        OrderDetailVO detailVO = orderService.getOrderDetail(userId, orderNumber);
        return Result.success(detailVO);
    }

    /**
     * 用户取消订单 (FR-OM-005)
     * @param orderNumber 要取消的子订单号
     * @return 操作结果
     */
    @PutMapping("/{orderNumber}/cancel") // 使用 PUT 请求更符合 RESTful 风格
    public Result<Void> cancelOrder(@PathVariable String orderNumber) {
        Long userId = BaseContext.getCurrentId();
        orderService.cancelOrder(userId, orderNumber);
        return Result.success();
    }

}
