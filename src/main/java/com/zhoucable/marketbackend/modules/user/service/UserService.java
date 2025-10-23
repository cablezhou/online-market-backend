package com.zhoucable.marketbackend.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.*;
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

    /**
     * 修改用户信息
     * @param updateDTO 修改的信息
     * @Date 2025/10/22 10:18
     */
    void updateProfile(UserProfileUpdateDTO updateDTO);

    /**
     * 发送短信验证码
     * @param smsCodeDTO
     * @Date 2025.10.22 11:21
     */
    void sendSmsCode(SmsCodeDTO smsCodeDTO);

    /**
     * 通过短信验证码登录
     * @param smsLoginDTO
     * @return 登录成功的信息以及Token
     * @Date 2025.10.22 11:22
     */
    UserLoginVO loginWithSms(SmsLoginDTO smsLoginDTO);

    /**
     * 重置密码
     */
    void resetPassword(ResetPasswordDTO resetPasswordDTO);

    /**
     * 更改用户角色（通常由管理员操作，使用场景为通过用户成为商家的申请）
     * @param userId 用户Id
     * @param role 新角色（0：普通用户，1：商家，2：管理员）
     */
    void changeUserRole(Long userId, Integer role);
}
