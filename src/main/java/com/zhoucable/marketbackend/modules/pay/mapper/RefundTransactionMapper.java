package com.zhoucable.marketbackend.modules.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.pay.entity.RefundTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款流水的mapper接口
 */
@Mapper
public interface RefundTransactionMapper extends BaseMapper<RefundTransaction> {
}
