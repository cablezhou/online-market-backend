package com.zhoucable.marketbackend.modules.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户申请成为商家时需要提供店铺名和介绍
 * @author 周开播
 * @Date 2025/10/22 16:43
 */
@Data
public class MerchantApplyDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Length(max = 20, message = "店铺名称不能超过20个字符")
    private String storeName;

    @Length(max = 200, message = "店铺介绍介绍不能超过200个字符")
    private String description;
}
