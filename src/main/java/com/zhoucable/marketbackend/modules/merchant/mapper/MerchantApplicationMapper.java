package com.zhoucable.marketbackend.modules.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作店铺申请表
 */
@Mapper
public interface MerchantApplicationMapper extends BaseMapper<MerchantApplication> {
}
