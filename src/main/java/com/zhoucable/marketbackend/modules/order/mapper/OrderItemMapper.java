package com.zhoucable.marketbackend.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细mapper接口
 * @author 周开播
 * @Date 2025年10月29日10:24:30
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

}
