package com.zhoucable.marketbackend.modules.shoppingcart.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览
 * 结构与CartVO类似，但只包含选中的商品
 */
@Data
public class CheckOutPreviewVO {

    private List<CartItemVO> items; //选中的商品项列表
    private BigDecimal totalPrice;
    private Integer totalQuantity; //选中的商品总件数

    //构造函数用于业务逻辑组装
    public CheckOutPreviewVO(List<CartItemVO> items){
        this.items = items;
        this.totalPrice = calculateTotalPrice(items);
        this.totalQuantity = calculateTotalQuantity(items);

    }

    private BigDecimal calculateTotalPrice(List<CartItemVO> items){
        return items.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateTotalQuantity(List<CartItemVO> items){
        return items.stream()
                .mapToInt(CartItemVO::getQuantity)
                .sum();
    }
}
