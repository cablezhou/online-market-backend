package com.zhoucable.marketbackend.modules.review.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 提交评价DTO
 * FR-PR-001
 * @author 周开播
 * @Date 2025年11月3日14:31:30
 */
@Data
public class SubmitReviewDTO {

    /**
     * 要评级的订单明细项id（order_item.id）
     */
    @NotNull(message = "订单项id不能为空")
    private Long orderItemId;

    @NotNull(message = "必须提供星级")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;

    @Length(max = 1000, message = "评价内容不能超过1000字符")
    private String content;

    private List<String> images;

}
