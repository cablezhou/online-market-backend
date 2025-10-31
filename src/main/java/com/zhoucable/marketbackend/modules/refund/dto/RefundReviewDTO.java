package com.zhoucable.marketbackend.modules.refund.dto;

import com.zhoucable.marketbackend.modules.admin.dto.ReviewActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 商家审核售后单DTO
 * @author 周开播
 * @Date 2025年10月31日14:59:39
 */
@Data
public class RefundReviewDTO {

    @NotNull(message = "必须指定操作类型")
    private ReviewAction action; //APPROVE或REJECT

    @Length(max = 200, message = "拒绝原因不能超过200个字符")
    private String reason; // 拒绝理由（当action为REJECT时必填）

    public enum ReviewAction {
        APPROVE,
        REJECT
    }
}
