package com.zhoucable.marketbackend.common;

import lombok.Data;

/**
 * 用于分页查询的DTO
 * @author 周开播
 * @Date 2025年10月24日10:39:58
 */
@Data
public class PageQueryDTO {

    //@Min(1) //可以添加校验
    private int page = 1; //默认页码

    //@Min(1) //同上
    private int size = 10; //默认每页数量
}
