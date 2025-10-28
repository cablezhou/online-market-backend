package com.zhoucable.marketbackend.modules.shoppingcart.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.shoppingcart.Service.CartService;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.AddItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.CheckOutItemsDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.UpdateItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartItemVO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartVO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CheckOutPreviewVO;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 * @author 周开播
 * @Date 2025年10月28日11:07:21
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;


    /**
     * 添加商品到购物车（FR-SC-001）
     * @param addItemDTO
     * @return
     */
    @PostMapping
    public Result<Void> addItem(@Valid @RequestBody AddItemDTO addItemDTO){
        Long userId = BaseContext.getCurrentId();
        cartService.addItem(addItemDTO,userId);
        return Result.success();
    }

    /**
     * 购物车列表展示（FR-SC-002）
     * @Date 2025年10月28日14:17:34
     */
    @GetMapping
    public Result<CartVO> getCart(){
        Long userId = BaseContext.getCurrentId();
        CartVO cartVO = cartService.getCartView(userId);
        return Result.success(cartVO);
    }

    /**
     * 修改购物车商品数量（FR-SC-003）
     */
    @PutMapping("/items/{cartItemId}")
    public Result<Void> updateItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateItemDTO updateDTO
            ){
        Long userId = BaseContext.getCurrentId();
        cartService.updateItemQuantity(cartItemId, updateDTO, userId);
        return Result.success();
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/items/{cartItemId}")
    public Result<Void> deleteItem(@PathVariable Long cartItemId){
        Long userId = BaseContext.getCurrentId();
        cartService.deleteItem(cartItemId, userId);
        return Result.success();
    }

    /**
     * 获取结算预览信息（FR-SC-004）
     */
    @PostMapping("/checkout-preview")
    public Result<CheckOutPreviewVO> getCheckOutPreview(
            @Valid @RequestBody CheckOutItemsDTO checkOutItemsDTO
            ){
        Long userId = BaseContext.getCurrentId();
        CheckOutPreviewVO previewVO = cartService.getCheckOutPreview(checkOutItemsDTO, userId);
        return Result.success(previewVO);
    }
}
