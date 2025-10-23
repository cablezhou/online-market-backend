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
 * 确保只有商家用户能调用创建新店铺的接口
 * @author 周开播
 * @Date 2025年10月23日11:32:50
 */
@Component
public class MerchantAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 获取用户ID (由 AuthInterceptor 保证存在且有效)
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            // 通常不会发生，因为 AuthInterceptor 会先拦截
            throw new BusinessException(4012, "请先登录");
        }

        // 2. 查询用户角色
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(4012, "用户不存在");
        }

        // 3. 校验角色是否为商家
        if (user.getRole() != 1) { // 1 代表商家
            throw new BusinessException(4031, "权限不足，只有商家才能执行此操作");
        }

        // 是商家，放行
        return true;
    }
}
