package com.zhoucable.marketbackend.modules.review.controller;


import com.zhoucable.marketbackend.common.PageQueryDTO;
import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.review.VO.ReviewVO;
import com.zhoucable.marketbackend.modules.review.dto.SubmitReviewDTO;
import com.zhoucable.marketbackend.modules.review.service.ReviewService;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评价模块控制器（用户端）
 * @author 周开播
 * @Date 2025年11月3日15:05:42
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 用户发表评价（FR-PR-001）
     * @param reviewDTO
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> submitReview(@Valid @RequestBody SubmitReviewDTO reviewDTO){
        Long userId = BaseContext.getCurrentId();
        reviewService.submitReview(userId, reviewDTO);
        return Result.success();
    }

    @GetMapping("/{id}/reviews")
    public Result<PageResult<ReviewVO>> getReviews(
            @PathVariable("id") Long id,
            PageQueryDTO pageQueryDTO
    ){
        PageResult<ReviewVO> pageResult = reviewService.getReviewsByProductId(id, pageQueryDTO);
        return Result.success(pageResult);
    }
}
