package com.zhoucable.marketbackend.modules.admin.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员对用户成为商家申请的审核操作
 * “通过”、“拒绝”（及原因）
 * @author 周开播
 * @Date 2025/10/23 9:29
 */
@Data
public class AdminReviewDTO {

    @NotNull(message = "操作类型不能为空")
    private ReviewActionType action; // ”approve“ 或 ”reject“

    private String reason; //拒绝理由，“action” 为 ”reject“ 时可选
}
