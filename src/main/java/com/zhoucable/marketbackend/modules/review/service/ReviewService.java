package com.zhoucable.marketbackend.modules.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.common.PageQueryDTO;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.modules.review.VO.ReviewVO;
import com.zhoucable.marketbackend.modules.review.dto.SubmitReviewDTO;
import com.zhoucable.marketbackend.modules.review.entity.ProductReview;

/**
 * 商品评价服务接口
 * @author 周开播
 * @Date 2025年11月3日14:43:03
 */
public interface ReviewService extends IService<ProductReview> {

    /**
     * FR-PR-001
     * 用户提交评价
     * @param userId 发表评价的用户的id
     * @param reviewDTO 评价内容
     */
    void submitReview(Long userId, SubmitReviewDTO reviewDTO);


    /**
     * FR-PR-002
     * 分页查询商品评价
     * @param productId 商品ID
     * @param pageQueryDTO 分页参数
     * @return 评价列表
     */
    PageResult<ReviewVO> getReviewsByProductId(Long productId, PageQueryDTO pageQueryDTO);
}
