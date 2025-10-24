package com.zhoucable.marketbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 封装分页结果
 * @author 周开播
 * @Date 2025年10月24日10:46:58
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total; //总记录数

    private List<T> list; //当前页数据列表
}
