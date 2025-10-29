package com.zhoucable.marketbackend.modules.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户地址mapper接口
 * @author 周开播
 * @Date 2025年10月28日15:58:23
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

    /**
     * 将用户的所有地址设为非默认
     * （在用户设置默认地址时使用）
     * @param userId 用户id
     */
    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId}")
    void setAllNotDefault(@Param("userId") Long userId);
}
