package com.zhoucable.marketbackend.modules.order.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.order.entity.ParentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 父订单mapper接口
 * @author 周开播
 * @Date 2025年10月29日10:22:30
 */
@Mapper
public interface ParentOrderMapper extends BaseMapper<ParentOrder> {

}
