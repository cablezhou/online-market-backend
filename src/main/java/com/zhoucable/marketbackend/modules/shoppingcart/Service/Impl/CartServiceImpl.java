package com.zhoucable.marketbackend.modules.shoppingcart.Service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.service.ProductSkuService;
import com.zhoucable.marketbackend.modules.shoppingcart.Service.CartService;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.AddItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.CheckOutItemsDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.dto.UpdateItemDTO;
import com.zhoucable.marketbackend.modules.shoppingcart.entity.ShoppingCart;
import com.zhoucable.marketbackend.modules.shoppingcart.mapper.ShoppingCartMapper;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartItemVO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CartVO;
import com.zhoucable.marketbackend.modules.shoppingcart.vo.CheckOutPreviewVO;
import com.zhoucable.marketbackend.utils.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 */
@Service
@Slf4j
public class CartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements CartService {

    @Autowired
    private ProductSkuService skuService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long CART_CACHE_TTL_HOURS = 24;


    /**
     * 统一的删除缓存方法
     */
    private void deleteCartCache(Long userId){
        String cacheKey = RedisKeyUtil.getCartUserKey(userId);
        stringRedisTemplate.delete(cacheKey);
        log.info("购物车缓存删除：{}", cacheKey);
    }

    @Override
    public void addItem(AddItemDTO addItemDTO, Long userId){

        //1.验证SKU是否存在且在售
        ProductSku sku = skuService.getById(addItemDTO.getSkuId());
        if(sku == null || sku.getStatus() == 0){
            throw new BusinessException(400, "商品已失效");
        }

        //2.检查库存
        if(addItemDTO.getQuantity() > sku.getStock()){
            throw new BusinessException(400, "商品库存不足");
        }

        //3.查询购物车中是否已存在该sku
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId)
                .eq(ShoppingCart::getSkuId, addItemDTO.getSkuId());
        ShoppingCart cartItem = this.getOne(queryWrapper);

        if(cartItem != null){
            //已存在该商品，叠加数量
            int newQuantity = cartItem.getQuantity() + addItemDTO.getQuantity();

            //再次检查库存
            if(newQuantity > sku.getStock()){
                throw new BusinessException(400, "超出库存量");
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setUpdateTime(LocalDateTime.now());
            this.updateById(cartItem);

        }else{
            //不存在，执行insert
            cartItem = new ShoppingCart();
            cartItem.setUserId(userId);
            cartItem.setSkuId(addItemDTO.getSkuId());
            cartItem.setQuantity(addItemDTO.getQuantity());
            cartItem.setCreateTime(LocalDateTime.now());
            cartItem.setUpdateTime(LocalDateTime.now());
            this.save(cartItem);
        }

        //4.删除Redis缓存
        deleteCartCache(userId);

    }

    /**
     * 获取购物车列表（FR-SC-002)
     */
    @Override
    public CartVO getCartView(Long userId){

        String cacheKey = RedisKeyUtil.getCartUserKey(userId);

        //1.尝试从Redis获取
        try{
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if(cachedJson != null && !cachedJson.isEmpty()){
                log.info("购物车缓存命中：{}", cacheKey);
                //2.命中，反序列化并返回
                return objectMapper.readValue(cachedJson, CartVO.class);
            }
        }catch (JsonProcessingException e){
            log.error("反序列化购物车缓存失败: key={}", cacheKey, e);
        }catch (Exception e){
            log.error("读取Redis缓存失败", e);
        }

        //3.未命中，查询数据库
        log.info("购物车缓存未命中，查询数据库: {}", userId);
        List<CartItemVO> cartItems = cartMapper.getCartItemsByUserId(userId);

        //4.组装CartVO
        CartVO cartVO = new CartVO(cartItems);

        //5.存入Redis
        try{
            String jsonToCache = objectMapper.writeValueAsString(cartVO);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    jsonToCache,
                    Duration.ofHours(CART_CACHE_TTL_HOURS) //设置24小时过期（也可更改）
            );
            log.info("购物车写入缓存: {}, TTL: {}h", cacheKey, CART_CACHE_TTL_HOURS);
        }catch (JsonProcessingException e){
            log.error("序列化购物车失败，userId={}", userId, e);
        }catch (Exception e){
            log.error("写入Redis缓存失败", e);
        }

        return cartVO;
    }

    /**
     * 修改购物车商品数量
     */
    @Override
    public void updateItemQuantity(Long cartItemId, UpdateItemDTO updateDTO, Long userId){

        //1.验证归属
        ShoppingCart cartItem = this.getById(cartItemId);
        if(cartItem == null || !Objects.equals(cartItem.getUserId(), userId)){
            throw new BusinessException(403,"无权操作");
        }

        //2.检查库存
        ProductSku sku = skuService.getById(cartItem.getSkuId());
        if(sku == null || sku.getStatus() == 0){
            throw new BusinessException(400,"商品已失效");
        }

        Integer newQuantity = updateDTO.getQuantity();
        if(newQuantity > sku.getStock()){
            throw new BusinessException(400, "库存不足，当前仅剩" + sku.getStock() + "件");
        }

        //3.更新数据库
        cartItem.setQuantity(newQuantity);
        cartItem.setUpdateTime(LocalDateTime.now());
        this.updateById(cartItem);

        //4.删除缓存
        deleteCartCache(userId);
    }

    /**
     * 删除购物车商品
     */
    @Override
    public void deleteItem(Long cartItemId, Long userId){

        //1.验证归属
        ShoppingCart cartItem = this.getById(cartItemId);
        if(cartItem == null || !Objects.equals(cartItem.getUserId(), userId)){
            throw new BusinessException(403,"无权操作");
        }

        //2.删除数据库
        this.removeById(cartItemId);

        //3.删除缓存
        deleteCartCache(userId);
    }

    /**
     * 获取结算预览信息（FR-SC-004）
     */
    @Override
    public CheckOutPreviewVO getCheckOutPreview(CheckOutItemsDTO checkOutItemsDTO, Long userId){

        List<Long> cartItemsIds = checkOutItemsDTO.getCartItemIds();

        //1.根据ID列表查询
        //mapper方法已包含userId校验
        List<CartItemVO> items = cartMapper.getCartItemsByIds(userId, cartItemsIds);

        //校验查询结果是否与请求id数量一致，防止传入不属于该用户的商品
        if(items.size() != cartItemsIds.size()){
            log.warn("用户 {} 尝试结算不属于自己的购物车项", userId);
            List<Long> foundIds = items.stream().map(CartItemVO::getCartItemId).toList();
            cartItemsIds.removeAll(foundIds); //差集，移除foundIds之外的
            throw new BusinessException(403, "包含无效或无权限的购物车项ID：" + cartItemsIds);
        }

        //2.再次校验库存
        for (CartItemVO item : items){
            if(item.getQuantity() > item.getStock()){
                throw new BusinessException(400, "商品[" + item.getProductName() + "] 库存不足");
            }
        }

        return new CheckOutPreviewVO(items);
    }

}
