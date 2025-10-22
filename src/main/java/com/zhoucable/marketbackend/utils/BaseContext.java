package com.zhoucable.marketbackend.utils;


/**
 * 工具类，用于传递用户ID，避免在每个方法参数里都写userId
 * @author 周开播
 * @Date 2025/10/22 9:57
 */
public class BaseContext {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }

    public static void removeCurrentId(){
        threadLocal.remove();
    }

}
