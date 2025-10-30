package com.zhoucable.marketbackend.modules.order.dto;


import com.zhoucable.marketbackend.common.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询订单列表的DTO（包含分页和状态筛选）
 * @author 周开播
 * @Date 2025年10月29日16:43:54
 */
@Data
@EqualsAndHashCode(callSuper = true) //继承PageQueryDTO
public class OrderListQueryDTO extends PageQueryDTO {

    /**
     * 按订单状态筛选（可选）
     * 0：待支付，1：待发货，2：已发货，3：已完成，4：已取消，5：退款中，6：已退款
     */
    private Integer status;
}
