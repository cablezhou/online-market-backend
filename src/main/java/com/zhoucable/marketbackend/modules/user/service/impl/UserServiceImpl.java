package com.zhoucable.marketbackend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.user.VO.UserLoginVO;
import com.zhoucable.marketbackend.modules.user.dto.*;
import com.zhoucable.marketbackend.modules.user.entity.User;
import com.zhoucable.marketbackend.modules.user.mapper.UserMapper;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import com.zhoucable.marketbackend.utils.BaseContext;
import com.zhoucable.marketbackend.utils.JwtUtil;
import com.zhoucable.marketbackend.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.undertow.UndertowWebServer;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

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
                RedisKeyUtil.getLoginTokenKey(token),
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

    @Override
    public void sendSmsCode(SmsCodeDTO smsCodeDTO){
        String phone = smsCodeDTO.getPhone();
        SmsCodeType type = smsCodeDTO.getType();

        //如果用户是在重置密码，则先校验手机号是否已注册
        if(SmsCodeType.RESET_PASSWORD.equals(type)){
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper();
            queryWrapper.eq(User::getPhone, phone);
            if(this.count(queryWrapper) == 0){
                throw new BusinessException(4041, "该手机号未注册");
            }
        }

        //使用工具类生成key
        String limitKey = RedisKeyUtil.getSmsLimitKey(type, phone);

        //String limitKey = "sms:limit:" + phone + ":" + type; //Redis存储该手机号，用于检验一分钟内只能发一次

        //1.检查一分钟内是否已发送过（防刷）
        if(Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))){
            throw new BusinessException(4291, "验证码发送过于频繁，请稍后再试");
        }

        //2.模拟调用短信服务商API，生成6位随机数字
        //TODO:替换为真实的短信服务
        String code = RandomStringUtils.randomNumeric(6);
        System.out.println("====向手机 " + phone + " 发送 [" + type.name() + "]验证码 " + code + "（模拟）====");

        //3.将验证码存入Redis，5分钟内有效
        String codeKey = RedisKeyUtil.getSmsCodeKey(type, phone);
        //String codeKey = "sms:code:" + phone + ":" + type;
        redisTemplate.opsForValue().set(codeKey, code,5,TimeUnit.MINUTES);

        //4.设置一分钟的发送间隔锁
        redisTemplate.opsForValue().set(limitKey, "1", 1, TimeUnit.MINUTES);
    }

    @Override
    public UserLoginVO loginWithSms(SmsLoginDTO smsLoginDTO){
        String phone = smsLoginDTO.getPhone();
        String code = smsLoginDTO.getCode();
        String codeKey = RedisKeyUtil.getSmsCodeKey(SmsCodeType.LOGIN, phone);
        //String codeKey = "sms:code:" + phone + ":LOGIN";

        //1.从Redis中获取验证码并校验
        String correctCode = (String) redisTemplate.opsForValue().get(codeKey);
        if(correctCode == null){
            throw new BusinessException(4013, "验证码已过期");
        } else if (!correctCode.equals(code)) {
            throw new BusinessException(4013, "验证码错误！");
        }

        //2.验证成功后删除验证码防止重复使用
        redisTemplate.delete(codeKey);

        //3.根据手机号查询用户，判断是登录还是注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = this.getOne(queryWrapper);

        //4.若用户不存在，则自动注册
        if(user == null){
            user = new User();
            user.setPhone(phone);
            //为新用户生成一个随机的、安全的默认密码
            String randomPassword = generateStrongPassword(16);
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setNickname("用户" + phone.substring(7)); //默认昵称
            user.setRole(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            this.save(user);
        }

        //5.登录（或自动注册）后生成Token并存入Redis
        String token = jwtUtil.generateToken(user.getId());
        redisTemplate.opsForValue().set(
                "login:token:" + token,
                user.getId(),
                jwtUtil.getExpireTime(),
                TimeUnit.SECONDS
        );

        //封装VO并返回
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setNickname(user.getNickname());
        return loginVO;
    }

    /**
     * 用户若通过手机号+验证码来注册，
     * 自动为其生成一个符合强度要求的随机密码
     * @param length 密码总长度
     * @return 随机强密码
     */
    private String generateStrongPassword(int length) {
        // 1. 定义不同字符类型的源
        String upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseChars = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String allChars = upperCaseChars + lowerCaseChars + digits;

        // 2. 确保密码中至少包含一个大写字母、一个小写字母和一个数字
        StringBuilder passwordBuilder = new StringBuilder();
        passwordBuilder.append(RandomStringUtils.random(1, upperCaseChars));
        passwordBuilder.append(RandomStringUtils.random(1, lowerCaseChars));
        passwordBuilder.append(RandomStringUtils.random(1, digits));

        // 3. 填充剩余长度的随机字符
        int remainingLength = length - 3;
        passwordBuilder.append(RandomStringUtils.random(remainingLength, allChars));

        // 4. 打乱字符顺序，避免可预测性（例如，密码总是以大写、小写、数字开头）
        List<Character> chars = passwordBuilder.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(chars);

        return chars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO){
        String phone = resetPasswordDTO.getPhone();
        String code = resetPasswordDTO.getCode();
        String codeKey = RedisKeyUtil.getSmsCodeKey(SmsCodeType.RESET_PASSWORD,phone);
        //String codeKey = "sms:code:" + phone + ":RESET_PASSWORD";

        //1.校验验证码
        String correctCode = (String) redisTemplate.opsForValue().get(codeKey);
        if(correctCode == null){
            throw new BusinessException(4013, "验证码已过期");
        } else if (!correctCode.equals(code)) {
            throw new BusinessException(4013, "验证码错误");
        }

        //2.验证成功后立即删除验证码
        redisTemplate.delete(codeKey);

        //3.找到用户
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(User::getPhone, phone);

        //设置新密码并更新
        updateWrapper.set(User::getPassword, passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        updateWrapper.set(User::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(updateWrapper);
        if(!updated){
            throw new BusinessException(4041, "该手机号未注册");
        }

    }
}
