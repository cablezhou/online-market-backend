package com.zhoucable.marketbackend.modules.user.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.*;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制类
 * @author 周开播
 * @Date 2025/10/21 15:34
 */

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO registerDTO){

        //调用service层处理注册逻辑
        userService.register(registerDTO);

        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO loginDTO){
        UserLoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserProfileUpdateDTO updateDTO){
        userService.updateProfile(updateDTO);
        return Result.success();
    }


    /**
     * 发送验证码
     * @param smsCodeDTO 只包含手机号的DTO
     * @return 成功信息（验证码在控制台打印）
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeDTO smsCodeDTO){
        userService.sendSmsCode(smsCodeDTO);
        return Result.success();
    }

    /**
     * 用验证码登录（未登录自动注册）
     * @param smsLoginDTO 带验证码和手机号的DTO
     * @return 登陆成功VO（带Token）
     */
    @PostMapping("/login/sms")
    public Result<UserLoginVO> loginWithSms(@Valid @RequestBody SmsLoginDTO smsLoginDTO){
        UserLoginVO loginVO = userService.loginWithSms(smsLoginDTO);
        return Result.success(loginVO);
    }


    /**
     * 重置密码
     * @param resetPasswordDTO 携带新密码的DTO
     * @return 操作结果
     * @author 周开播
     * @Date 2025/10/22 15:41
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO){
        userService.resetPassword(resetPasswordDTO);
        return Result.success();
    }
}
