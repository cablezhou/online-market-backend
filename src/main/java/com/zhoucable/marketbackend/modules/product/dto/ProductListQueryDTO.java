package com.zhoucable.marketbackend.modules.product.dto;

import com.zhoucable.marketbackend.common.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用于查询商品列表的请求DTO
 * @author 周开播
 * @Date 2025年10月24日10:41:38
 */
@Data
@EqualsAndHashCode(callSuper = true) //继承需要
public class ProductListQueryDTO extends PageQueryDTO {

    private Long categoryId; //按指定分类进行查询

    private Integer level; //category层级（1,2,3）

    //搜索关键字
    private String keyWord;

    //排序字段（e.g., "sales", "price", "createTime"）
    private String sortField;

    private String sortOrder = "desc"; // "asc" or "desc"
}
