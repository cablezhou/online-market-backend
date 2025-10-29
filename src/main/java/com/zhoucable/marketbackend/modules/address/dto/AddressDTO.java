package com.zhoucable.marketbackend.modules.address.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 添加或修改地址时的DTO
 * @author 周开播
 * @Date 2025年10月28日16:01:16
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收件人姓名不能为空")
    @Length(max = 50, message = "收件人姓名过长")
    private String recipientName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^(1[3-9]\\d{9}|(0\\d{2,3}-)?\\d{7,8})$", message = "联系电话格式不正确")
    private String phone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区、县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Length(max = 255, message = "详细地址过长")
    private String detailAddress;

    @NotNull(message = "必须指定是否为默认地址")
    private Boolean isDefault;
}
