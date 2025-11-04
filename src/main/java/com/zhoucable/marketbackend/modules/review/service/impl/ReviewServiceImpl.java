package com.zhoucable.marketbackend.modules.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.common.PageQueryDTO;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.order.entity.Order;
import com.zhoucable.marketbackend.modules.order.entity.OrderItem;
import com.zhoucable.marketbackend.modules.order.mapper.OrderItemMapper;
import com.zhoucable.marketbackend.modules.order.mapper.OrderMapper;
import com.zhoucable.marketbackend.modules.product.service.ProductService;
import com.zhoucable.marketbackend.modules.review.VO.ReviewVO;
import com.zhoucable.marketbackend.modules.review.dto.SubmitReviewDTO;
import com.zhoucable.marketbackend.modules.review.entity.ProductReview;
import com.zhoucable.marketbackend.modules.review.mapper.ProductReviewMapper;
import com.zhoucable.marketbackend.modules.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ReviewService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Override
    @Transactional
    public void submitReview(Long userId, SubmitReviewDTO reviewDTO){

        //1.查询订单项，确认是否存在
        OrderItem orderItem = orderItemMapper.selectById(reviewDTO.getOrderItemId());
        if(orderItem == null){
            log.warn("用户 {} 尝试评价不存在的订单项ID：{}", userId, reviewDTO.getOrderItemId());
            throw new BusinessException(404,"订单项不存在");
        }

        //2.校验订单归属
        Order order = orderMapper.selectById(orderItem.getOrderId());
        if(!Objects.equals(order.getUserId(), userId)){
            log.warn("用户 {} 尝试评价不属于自己的订单：{}", userId, order.getId());
            throw new BusinessException(403,"无权评价此商品");
        } else if (order == null) {
            throw new BusinessException(404,"子订单不存在");
        }

        //3.检查订单状态是否为“已完成”（status=3）
        if(!Objects.equals(order.getStatus(), 3)){
            log.warn("用户 {} 尝试评价未完成的订单：{}", userId, order.getId());
            throw new BusinessException(409,"订单尚未完成，无法评价");
        }

        //4.检查订单项是否已评价过（review_status = 0）
        if(!Objects.equals(orderItem.getReviewStatus(), 0)){
            log.warn("用户 {} 尝试重复评价订单项：{}", userId, orderItem.getId());
            throw new BusinessException(409,"您已评价过此商品");
        }

        //5.更新订单项状态为“已评价”
        LambdaUpdateWrapper<OrderItem> itemUpdateWrapper = new LambdaUpdateWrapper<>();
        itemUpdateWrapper.eq(OrderItem::getId, orderItem.getId())
                .eq(OrderItem::getReviewStatus, 0) //乐观锁，防止并发重复评价
                .set(OrderItem::getReviewStatus, 1);

        int affectedRows = orderItemMapper.update(itemUpdateWrapper);

        if(affectedRows == 0){
            // 理论上如果 review_status 检查通过了，这里不应该失败，除非有并发
            log.warn("评价并发冲突或订单项状态已改变：{}", orderItem.getId());
            throw new BusinessException(409,"您已评价过此商品");
        }

        //6.插入评价表
        ProductReview review = new ProductReview();
        BeanUtils.copyProperties(reviewDTO, review); //拷贝rating、content、images

        review.setUserId(userId);
        review.setStoreId(review.getStoreId());
        review.setOrderId(orderItem.getOrderId());
        review.setOrderItemId(orderItem.getId());
        review.setProductId(orderItem.getProductId());
        review.setSkuId(orderItem.getSkuId());

        //TODO:增加评价审核功能
        review.setStatus(1); //默认为“显示”，若日后评价需要审核，这里先设置为0：“待审核”
        review.setCreateTime(LocalDateTime.now());

        productReviewMapper.insert(review);

        //7.TODO:异步更新商品表的评价总数和平均分(该字段还未添加)
        log.info("TODO: 异步更新商品 {} 的评价统计...", orderItem.getProductId());

        log.info("用户 {} 评价订单项 {} 成功，评价ID: {}", userId, orderItem.getId(), review.getId());

    }

    @Override
    public PageResult<ReviewVO> getReviewsByProductId(Long productId, PageQueryDTO pageQueryDTO){


        //1.创建分页对象
        Page<ReviewVO> page = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getSize());

        //2.调用Mapper中的自定义查询
        productReviewMapper.selectReviewPage(page, productId);

        List<ReviewVO> records = page.getRecords();

        // (records 中每个 VO 的 getSpecifications() 方法将在
        //  Jackson 序列化时被自动调用，完成 JSON 解析)

        return new PageResult<>(page.getTotal(), records);
    }
}
