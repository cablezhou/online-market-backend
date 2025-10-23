package com.zhoucable.marketbackend.config;


import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.user.entity.User;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员拦截器，对管理员操作进行拦截
 * 用于检查用户是否是管理员
 * @author 周开播
 * @Date 2025年10月23日10:24:28
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{

        //1. 从ThreadLocal中获取用户ID（AuthInterceptor放进去的）
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            // 如果 AuthInterceptor 没放行（例如白名单接口），这里可能是 null
            // 但因为拦截顺序设置，Admin 接口肯定不是白名单，所以 userId 必须存在，这里只是做个样子
            throw new BusinessException(4012, "请先登录");
        }

        //2.查询用户的角色（看是否为管理员）
        User user = userService.getById(userId);
        if(user == null){
            throw new BusinessException(4012, "用户不存在");
        }

        if(user.getRole() != 2){ //2为管理员
            throw new BusinessException(4031,"权限不足，禁止访问");
        }

        //是管理员，放行
        return true;
    }
    // afterCompletion 不需要，BaseContext 的清理由 AuthInterceptor 统一负责
}
