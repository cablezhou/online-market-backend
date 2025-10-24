package com.zhoucable.marketbackend.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作分类表
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
