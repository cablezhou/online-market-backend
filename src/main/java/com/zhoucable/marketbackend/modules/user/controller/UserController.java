package com.zhoucable.marketbackend.modules.user.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.UserLoginDTO;
import com.zhoucable.marketbackend.modules.user.dto.UserProfileUpdateDTO;
import com.zhoucable.marketbackend.modules.user.dto.UserRegisterDTO;
import com.zhoucable.marketbackend.modules.user.service.UserService;
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
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO){

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
}
