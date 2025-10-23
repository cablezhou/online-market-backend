package com.zhoucable.marketbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){

        // 1. 注册“身份认证”拦截器 (检查是否登录)
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 /api 开头的请求
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login", //放行登录和注册接口
                        "/api/user/sms-code",
                        "/api/user/login/sms",
                        "/api/user/reset-password"
                );

        //2. 注册“管理员权限”拦截器（检查操作是否为管理员进行）
        // 必须在authInterceptor之后
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
