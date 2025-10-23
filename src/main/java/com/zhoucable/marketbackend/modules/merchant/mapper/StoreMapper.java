package com.zhoucable.marketbackend.modules.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作店铺表
 */
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
