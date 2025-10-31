package com.zhoucable.marketbackend.modules.refund.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.refund.entity.RefundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后单Mapper接口
 * @author 周开播
 * @Date 2025年10月31日14:52:57
 */
@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {
}
