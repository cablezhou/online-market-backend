package com.zhoucable.marketbackend.modules.product.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品分类表
 * @author 周开播
 * @Date 2025年10月23日15:53:00
 */
@Data
@TableName("category")
public class Category {

    private Long id;

    private Long parentId; //父分类id，顶级分类此项为0

    private String name;

    private Integer level; //分类级别

    private Integer sortOrder; //排序字段，数字越小越靠前（可选）
}
