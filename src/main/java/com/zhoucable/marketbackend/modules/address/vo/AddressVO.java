package com.zhoucable.marketbackend.modules.address.vo;


import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import lombok.Data;

/**
 * 用于向前端返回地址列表的VO
 * @author 周开播
 * @Date 2025年10月28日16:05:30
 */
@Data
public class AddressVO {
    private Long id;
    private String recipientName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault; //返回true/false给前端

    /**
     * 实体转换
     */
    public static AddressVO fromEntity(UserAddress addressEntity){
        AddressVO vo = new AddressVO();
        vo.setId(addressEntity.getId());
        vo.setRecipientName(addressEntity.getRecipientName());
        vo.setPhone(addressEntity.getPhone());
        vo.setProvince(addressEntity.getProvince());
        vo.setCity(addressEntity.getCity());
        vo.setDistrict(addressEntity.getDistrict());
        vo.setDetailAddress(addressEntity.getDetailAddress());
        vo.setIsDefault(addressEntity.getIsDefault() == 1); //得到true或false
        return vo;
    }
}
