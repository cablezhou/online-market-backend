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

    @Autowired
    private MerchantAuthInterceptor merchantAuthInterceptor;

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

        //3.商家权限拦截器（检查Role = 1）
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/stores/**"); // 拦截商家创建店铺及未来可能的店铺管理接口
        // "/api/merchant/apply" 不需要商家权限，普通用户就能申请，所以不用加在这里
    }
}
