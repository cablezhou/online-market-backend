package com.zhoucable.marketbackend.modules.shoppingcart.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 返回给前端的完整购物车视图
 * @author 周开播
 * @Date 2025年10月28日11:24:26
 */
@Data
public class CartVO {

    private List<CartItemVO> items; //商品项列表


    public CartVO(List<CartItemVO> items) {
        this.items = items;
    }

}

   //不再需要这两个部分，因为数量和总价是由前端来动态计算的，不是后端。
/*    private BigDecimal calculateTotalPrice(List<CartItemVO> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateTotalQuantity(List<CartItemVO> items){
        return items.stream()
                .mapToInt(CartItemVO::getQuantity)
                .sum();
    }
}*/
