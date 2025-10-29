package com.zhoucable.marketbackend.modules.address.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zhoucable.marketbackend.modules.address.dto.AddressDTO;
import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import com.zhoucable.marketbackend.modules.address.vo.AddressVO;

import java.util.List;

/**
 * 地址管理服务接口
 * @author 周开播
 * @Date 2025年10月28日16:10:30
 */
public interface AddressService extends IService<UserAddress> {

    /**
     * 添加收货地址（FR-AM-001)
     */
    void addAddress(AddressDTO addressDTO, Long userId);

    /**
     * 查看售货地址列表 （FR-AM-004）
     * @param userId 用户id
     * @return 收货地址列表
     */
    List<AddressVO> getAddressList(Long userId);

    /**
     * 修改收货地址信息 （FR-AM-002）
     * @param addressId 收货地址id
     * @param addressDTO 相关dto
     * @param userId 用户id
     */
    void updateAddress(Long addressId, AddressDTO addressDTO, Long userId);

    /**
     * 删除收货地址 （FR-AM-003）
     * @param addressId 收货地址id
     * @param userId 用户id
     */
    void deleteAddress(Long addressId, Long userId);

    /**
     * 设置默认地址 （FR-AM-005）
     * @param addressId 收货地址id
     * @param userId 用户id
     */
    void setDefaultAddress(Long addressId, Long userId);
}
