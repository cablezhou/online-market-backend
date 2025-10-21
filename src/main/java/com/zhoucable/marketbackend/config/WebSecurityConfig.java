package com.zhoucable.marketbackend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    /**
     * 配置密码编码器，将其注册为Spring Bean
     * @return BCrypt密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链，暂时允许所有匿名请求，以便API调试
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain实例
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) //允许所有请求
                .csrf(csrf -> csrf.disable()); //禁用csrf保护，便于postman测试
        return http.build();

    }
}
