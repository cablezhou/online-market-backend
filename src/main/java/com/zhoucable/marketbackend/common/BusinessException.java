package com.zhoucable.marketbackend.common;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 自定义业务异常
 * @author 周开播
 * 2025/10/21/15:48
 */

@Getter
public class BusinessException extends RuntimeException{

    private final Integer code;

    public BusinessException(Integer code, String message){
        super(message);
        this.code = code;
    }
}
