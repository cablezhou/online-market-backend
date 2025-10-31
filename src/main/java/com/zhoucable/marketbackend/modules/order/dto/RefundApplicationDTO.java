package com.zhoucable.marketbackend.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户申请退款DTO
 * @author 周开播
 * @Date 2025年10月30日16:47:05
 */
@Data
public class RefundApplicationDTO {
    @NotBlank(message = "退款原因不能为空")
    @Length(max = 200, message = "退款原因不能超过200个字符")
    private String reason;
}
