package com.zhoucable.marketbackend.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.UserLoginDTO;
import com.zhoucable.marketbackend.modules.user.dto.UserRegisterDTO;
import com.zhoucable.marketbackend.modules.user.entity.User;

/**
 * 用户服务类
 * @author 周开播
 * 2025/10/21 15:18
 */

public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param registerDTO 注册信息
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO
     */
    UserLoginVO login(UserLoginDTO loginDTO);
}
