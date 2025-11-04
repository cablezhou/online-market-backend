package com.zhoucable.marketbackend.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhoucable.marketbackend.modules.review.VO.ReviewVO;
import com.zhoucable.marketbackend.modules.review.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

/**
 * 商品评价mapper接口
 * @author 周开播
 * @Date 2025年11月3日14:38:53
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /**
     * 分页连表查询评价列表（FR-PR-002）
     * 联结: product_review (r) -> user (u) -> order_item (oi)
     */
    @Select("SELECT " +
            "r.rating, r.content, r.images, r.create_time, " +
            "u.nickname, " +
            // 将 order_item 的 sku_snapshot 字段(JSON类型) 转换为字符串，以便VO接收
            "CAST(oi.sku_snapshot AS CHAR) as skuSnapshotJson " +
            "FROM product_review r " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "LEFT JOIN order_item oi ON r.order_item_id = oi.id " +
            "WHERE r.product_id = #{productId} AND r.status = 1 " + // 只显示已审核的
            "ORDER BY r.create_time DESC")
    Page<ReviewVO> selectReviewPage(Page<ReviewVO> page, @Param("productId") Long productId);
}
