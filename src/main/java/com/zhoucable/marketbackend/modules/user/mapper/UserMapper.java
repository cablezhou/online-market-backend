package com.zhoucable.marketbackend.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表的mapper接口，继承BaseMapper
 * @author 周开播
 * 2025/10/21 15:14
 */


@Mapper
public interface UserMapper extends BaseMapper<User> {
    //暂时无需添加额外方法，BaseMapper已够用
}
