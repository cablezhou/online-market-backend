package com.zhoucable.marketbackend.modules.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.pay.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付流水的mapper接口
 */
@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {
}
