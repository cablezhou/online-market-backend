package com.zhoucable.marketbackend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，定义了两个处理器：一个处理自定义的BusinessException
 * 另一个处理所有其他未被捕获的Exception
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理JSR-303参数校验异常
     * @param e 校验异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        //从异常对象中获取第一个校验失败的错误提示
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败：{}", errorMessage);
        //返回符合设计文档定义的4001错误码
        return Result.error(4001, "参数校验失败：" + errorMessage);
    }



    /**
     * 处理自定义的业务异常
     * @param e 业务异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e){
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(),e.getMessage());
    }

    /**
     * 处理所有未被捕获的其他异常
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e){
        //记录详细的错误日志
        log.error("服务器内部错误：", e);
        //返回一个对用户友好的通用错误提示
        return Result.error(500, "服务器开小差了，请稍后再试");
    }
}
