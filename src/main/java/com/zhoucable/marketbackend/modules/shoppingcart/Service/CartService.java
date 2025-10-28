package com.zhoucable.marketbackend.modules.shoppingcart.Service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.AddItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.CheckOutItemsDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.UpdateItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.entity.ShoppingCart;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartVO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CheckOutPreviewVO;

/**
 * 购物车服务接口
 * @author 周开播
 * @Date 2025年10月28日10:54:33
 */
public interface CartService extends IService<ShoppingCart> {

    /**
     * 添加商品到购物车（FR-SC-001）
     * @param addItemDTO 添加商品DTO
     * @param userId 当前登录的用户的id
     */
    void addItem(AddItemDTO addItemDTO, Long userId);

    /**
     * 获取购物车列表视图（FR-SC-002）
     * @param userId 用户id
     * @return CartVO
     */
    CartVO getCartView(Long userId);

    /**
     * 修改购物车商品数量
     * @param cartItemId 购物车项ID
     * @param updateDTO 更新DTO
     * @param userId 用户id
     * @Date 2025年10月28日14:23:36
     */
    void updateItemQuantity(Long cartItemId, UpdateItemDTO updateDTO, Long userId );


    /**
     * 删除购物车商品
     * @param cartItemId 购物车项ID
     * @param userId 用户ID
     * @Date 2025年10月28日14:24:15
     */
    void deleteItem(Long cartItemId, Long userId);

    /**
     * 获取结算预览信息
     * @param checkOutItemsDTO 需结算的商品的id
     * @param userId 用户id
     * @return 返回结算预览视图
     */
    CheckOutPreviewVO getCheckOutPreview(CheckOutItemsDTO checkOutItemsDTO, Long userId);
}
