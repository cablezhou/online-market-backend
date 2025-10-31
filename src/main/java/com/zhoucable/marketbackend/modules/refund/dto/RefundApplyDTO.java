package com.zhoucable.marketbackend.modules.refund.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用于申请售后（退款/退货）DTO
 * @author 周开播
 * @Date 2025年10月31日14:57:31
 */
@Data
public class RefundApplyDTO {

    @NotNull(message = "订单项id不能为空")
    private Long orderItemId;

    @NotNull(message = "退款数量不能为空")
    @Min(value = 1, message = "退款数量至少为1")
    private Integer quantity;

    @NotBlank(message = "退款原因不能为空")
    @Length(max = 200, message = "退款原因不能超过200个字符")
    private String reason;

    // (可选) 还可以添加图片凭证等字段
    // private List<String> proofImages;
}
