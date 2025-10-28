package com.zhoucable.marketbackend.modules.shoppingcart.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhoucable.marketbackend.modules.shoppingcart.entity.ShoppingCart;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 购物车Mapper接口
 * @author 周开播
 * @Date 2025年10月28日10:46:07
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    /**
     * 根据用户ID查询购物车视图
     */
    /**
     * (新增) 根据用户ID查询购物车视图
     * (设计文档 来源 48)
     */
    @Select("SELECT " +
            "c.id AS cartItemId, " +
            "c.quantity, " +
            "s.id AS skuId, " +
            "s.price, " +
            "s.stock, " +
            "s.image AS skuImage, " +
            "s.specifications AS skuSpecifications, " +
            "p.id AS productId, " +
            "p.name AS productName, " +
            "p.main_image AS mainImage, " +
            "p.store_id AS storeId, " +
            "st.name AS storeName " +
            "FROM shopping_cart c " +
            "JOIN product_sku s ON c.sku_id = s.id " +
            "JOIN product p ON s.product_id = p.id " +
            "JOIN store st ON p.store_id = st.id " +
            "WHERE c.user_id = #{userId} " +
            "AND s.status = 1 AND p.status = 1 " + // 确保SKU和SPU都在售
            "ORDER BY c.create_time DESC")
    List<CartItemVO> getCartItemsByUserId(@Param("userId") Long userId);

    /**
     * 根据购物车ID列表查询视图 (用于结算)
     * (FR-SC-004)
     */
    @Select("<script>" +
            "SELECT " +
            "c.id AS cartItemId, " +
            "c.quantity, " +
            "s.id AS skuId, " +
            "s.price, " +
            "s.stock, " +
            "s.image AS skuImage, " +
            "s.specifications AS skuSpecifications, " +
            "p.id AS productId, " +
            "p.name AS productName, " +
            "p.main_image AS mainImage, " +
            "p.store_id AS storeId, " +
            "st.name AS storeName " +
            "FROM shopping_cart c " +
            "JOIN product_sku s ON c.sku_id = s.id " +
            "JOIN product p ON s.product_id = p.id " +
            "JOIN store st ON p.store_id = st.id " +
            "WHERE c.id IN " +
            "<foreach item='id' collection='cartItemIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND c.user_id = #{userId} " + // 确保是该用户的
            "AND s.status = 1 AND p.status = 1 " + // 确保SKU和SPU都在售
            "</script>")
    List<CartItemVO> getCartItemsByIds(@Param("userId") Long userId, @Param("cartItemIds") List<Long> cartItemIds);
}
