package com.zhoucable.marketbackend.modules.shoppingcart.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 结算预览DTO，包含选中的购物车项ID
 * @author 周开播
 * @Date 2025年10月28日14:38:35
 */
@Data
public class CheckOutItemsDTO {

    @NotEmpty(message = "请至少选择一项商品进行结算")
    private List<Long> cartItemIds;
}
