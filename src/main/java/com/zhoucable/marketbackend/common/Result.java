package com.zhoucable.marketbackend.common;




import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式封装类
 * @param <T> data部分的泛型类型
 */
@Data
@NoArgsConstructor
public class Result<T> {
    private Integer code; //业务状态码，200表示成功
    private String message; //提示信息
    private T data; //实际返回的数据

    public Result(Integer code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应的静态工厂方法
     * @param data 实际返回的数据
     * @param <T> 数据的类型
     * @return 包含成功状态和数据的Result对象
     */
    public static <T> Result<T> success(T data){
        return new Result<T>(200,"操作成功",data);
    }

    /**
     * 无数据返回的成功响应静态工厂方法
     * @return 包含成功状态的无数据的Result对象
     */
    public static Result<Void> success(){
        return new Result<Void>(200, "操作成功",null);
    }

    /**
     * 错误响应的静态工厂方法
     * @param code 错误码
     * @param message 错误信息
     * @return 包含错误状态和信息的Result对象
     */
    public static <T> Result<T> error(Integer code, String message){
        return new Result<T>(code, message, null);
    }
}
