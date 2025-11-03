package com.zhoucable.marketbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private MerchantAuthInterceptor merchantAuthInterceptor;

    //文件存储路径
    @Value("${file.upload.path}")
    private String uploadPath;

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
                        "/api/user/reset-password",
                        "/uploads/**", //放行对上传文件的访问
                        "/api/products/**", //放行公共商品列表和详情接口
                        //---放行支付部分---
                        "/api/pay/callback/**", //放行支付渠道真实回调（尚未实现）
                        "/api/mock/pay/**" //放行开发阶段模拟支付回调
                );

        //2. 检查操作是否为管理员进行
        // 必须在authInterceptor之后
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");

        //3.商家权限拦截器（检查Role = 1）
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/merchant/**") // 拦截商家创建店铺及未来可能的店铺管理接口，以及创建商品
                .excludePathPatterns("/api/merchant/apply"); //排除普通用户申请成为商家
    }

    /**
     * 配置静态资源映射
     * 将 URL 路径 /uploads/** 映射到磁盘上的 uploadPath 目录
     * @author 周开播
     * @Date 2025年10月23日15:27:13
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "file:" 前缀表示这是一个本地文件系统路径
        // 确保 uploadPath 后面有一个文件分隔符 (根据操作系统自动添加)
        String resourceLocation = "file:" + uploadPath.replace("\\", "/") + (uploadPath.endsWith("/") ? "" : "/");

        registry.addResourceHandler("/uploads/**") // 前端访问的 URL 路径模式
                .addResourceLocations(resourceLocation); // 映射到的服务器本地目录
    }
}
