package com.zhoucable.marketbackend.config;

import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.utils.BaseContext;
import com.zhoucable.marketbackend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户认证拦截器
 * @author 周开播
 * @Date 2025/10/22 9:59
 */

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{

        //1.从请求头获取Token
        String token = request.getHeader("Authorization");

        //2.验证Token
        if(token != null && token.startsWith("Bearer ")){
            token = token.substring(7); //去掉”Bearer “前缀
            Long userId = jwtUtil.validateTokenAndGetUserId(token);

            if(userId != null){
                //验证通过，将其存入ThreadLocal
                BaseContext.setCurrentId(userId);
                return true; //放行
            }
        }

        //4.验证失败，抛出异常
        throw new BusinessException(4012, "登录已过期，请重新登陆");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception{
        //请求处理完毕后，清理ThreadLocal，防止内存泄露
        BaseContext.removeCurrentId();
    }
}
