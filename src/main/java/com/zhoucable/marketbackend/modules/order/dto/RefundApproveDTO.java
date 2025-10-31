package com.zhoucable.marketbackend.modules.order.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家审核退款DTO
 * @author 周开播
 * @Date 2025年10月30日17:05:42
 */
@Data
public class RefundApproveDTO {
    @NotNull(message = "必须指定操作类型")
    private RefundActionType action; //APPROVE或REJECT

    private String reason; //拒绝理由（当action为REJECT时）
}
