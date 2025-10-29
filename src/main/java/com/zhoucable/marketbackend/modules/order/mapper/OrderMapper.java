package com.zhoucable.marketbackend.modules.order.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 子订单mapper接口
 * @author 周开播
 * @Date 2025年10月29日10:23:39
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

}
