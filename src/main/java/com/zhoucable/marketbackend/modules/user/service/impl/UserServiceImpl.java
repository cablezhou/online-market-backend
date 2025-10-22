package com.zhoucable.marketbackend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.UserLoginDTO;
import com.zhoucable.marketbackend.modules.user.dto.UserProfileUpdateDTO;
import com.zhoucable.marketbackend.modules.user.dto.UserRegisterDTO;
import com.zhoucable.marketbackend.modules.user.entity.User;
import com.zhoucable.marketbackend.modules.user.mapper.UserMapper;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import com.zhoucable.marketbackend.utils.BaseContext;
import com.zhoucable.marketbackend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.undertow.UndertowWebServer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder; //密码编码器

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(UserRegisterDTO registerDTO){
        //1.查询手机号是否已注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, registerDTO.getPhone());
        long count = this.count(queryWrapper); //Mybatis-Plus提供的方法

        //2.如果已注册，抛出异常
        if(count > 0){
            throw new BusinessException(4091, "该手机号已注册");
        }

        //3.创建新用户
        User newUser = new User();
        newUser.setPhone(registerDTO.getPhone());
        newUser.setNickname(registerDTO.getNickname());

        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
        newUser.setPassword(encodedPassword);

        newUser.setRole(0); //默认为普通用户
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        this.save(newUser);
    }

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO){

        //1.根据手机号查找用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, loginDTO.getPhone());
        User user = this.getOne(queryWrapper);

        //2.如果用户不存在，抛出异常
        if(user == null){
            throw new BusinessException(4011, "手机号或密码错误");
        }

        //3.校验密码
        boolean isMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        if(!isMatch){
            //密码不匹配，抛出异常
            throw new BusinessException(4011, "手机号或密码错误");
        }

        //4.登陆成功，准备返回用户信息
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setNickname(user.getNickname());

        //调用JwtUtil生成Token
        String token = jwtUtil.generateToken(user.getId());

        //5.将Token存入Redis，并设置与JWT相同的过期时间
        //key -> "login:token:<token>"
        //value -> user id
        //expire -> 7 days
        redisTemplate.opsForValue().set(
                "login:token:" + token,
                user.getId(),
                jwtUtil.getExpireTime(),
                TimeUnit.SECONDS
        );

        loginVO.setToken(token);

        return loginVO;
    }

    @Override
    public void updateProfile(UserProfileUpdateDTO updateDTO){
        // 1. 从ThreadLocal中获取当前登录用户的ID
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            //理论上拦截器会处理，这里双重保险
            throw new BusinessException(4012, "请先登录！");
        }

        //2.构建要更新的用户对象
        User userToUpdate = new User();
        userToUpdate.setId(userId);
        userToUpdate.setNickname(updateDTO.getNickname());
        userToUpdate.setUpdateTime(LocalDateTime.now());

        //3.调用Mybatis-Plus的更新方法
        this.updateById(userToUpdate);

        //TODO:设计文档要求更新用户信息的Redis缓存
        //目前置换存了Token，若后续把用户信息也存入Redis，这里要同步更新。
    }
}
