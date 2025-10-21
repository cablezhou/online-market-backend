package com.zhoucable.marketbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhoucable.marketbackend.modules.*.mapper") //扫描所有模块下的mapper，告诉Mybatis-Plus
public class OnlineMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineMarketApplication.class, args);
    }

}
