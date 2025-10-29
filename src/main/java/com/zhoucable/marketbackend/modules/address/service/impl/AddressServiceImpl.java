package com.zhoucable.marketbackend.modules.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.address.dto.AddressDTO;
import com.zhoucable.marketbackend.modules.address.entity.UserAddress;
import com.zhoucable.marketbackend.modules.address.mapper.UserAddressMapper;
import com.zhoucable.marketbackend.modules.address.service.AddressService;
import com.zhoucable.marketbackend.modules.address.vo.AddressVO;
import com.zhoucable.marketbackend.modules.user.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 地址管理服务实现
 * @author 周开播
 * @Date 2025年10月28日16:16:08
 */
@Service
public class AddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements AddressService {

    @Autowired
    private UserAddressMapper addressMapper;

    //地址数量上限
    private static final int MAX_ADDRESS_COUNT = 8;


    /**
     * 校验地址归属-内部工具方法
     */
    private void checkAddressOwnership(UserAddress address, Long currentUserId){
        if(address == null || !Objects.equals(address.getUserId(), currentUserId)){
            throw new BusinessException(403, "无权操作此地址");
        }
    }

    /**
     * 添加收货地址（FR-AM-001)
     */
    @Override
    @Transactional //涉及多个更新操作，开启事务
    public void addAddress(AddressDTO addressDTO, Long userId){

        //1.检查地址数量是否已达上限
        LambdaQueryWrapper<UserAddress> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserAddress::getUserId, userId);
        long count = this.count(countWrapper);
        if(count >= MAX_ADDRESS_COUNT){
            throw new BusinessException(400, "地址数量已达上限");
        }

        //2.如果设置为默认
        if(addressDTO.getIsDefault()){
            //先将其他所有地址设为非默认
            addressMapper.setAllNotDefault(userId);
        }

        //3.转换DTO为entity
        UserAddress newAddress = new UserAddress();
        BeanUtils.copyProperties(addressDTO, newAddress);
        newAddress.setUserId(userId);
        newAddress.setIsDefault(addressDTO.getIsDefault() ? 1 : 0); //boolean转int
        newAddress.setCreateTime(LocalDateTime.now());
        newAddress.setUpdateTime(LocalDateTime.now());

        this.save(newAddress);
    }

    /**
     * 查看收货地址列表（FR-AM-004）
     */
    @Override
    public List<AddressVO> getAddressList(Long userId){

        LambdaQueryWrapper<UserAddress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAddress::getUserId, userId);
        //按is_default降序、update_time降序排列
        queryWrapper.orderByDesc(UserAddress::getIsDefault, UserAddress::getUpdateTime);

        List<UserAddress> entities = this.list(queryWrapper);

        //转换为VO列表
        return entities.stream()
                .map(AddressVO::fromEntity)
                .toList();
    }

    /**
     * 修改收货地址（FR-AM-002）
     */
    @Override
    @Transactional
    public void updateAddress(Long addressId, AddressDTO addressDTO, Long userId){

        //1.校验地址归属
        UserAddress existingAddress = this.getById(addressId);
        checkAddressOwnership(existingAddress, userId);

        //2.如果设置为默认
        if(addressDTO.getIsDefault()){
            // 先将该用户其他所有地址设为非默认
            addressMapper.setAllNotDefault(userId);
        }

        //3.转换DTO为Entity并更新
        BeanUtils.copyProperties(addressDTO, existingAddress);
        existingAddress.setId(addressId); //确保id不变
        existingAddress.setUserId(userId); //确保userId不变
        existingAddress.setIsDefault(addressDTO.getIsDefault() ? 1 : 0);
        existingAddress.setUpdateTime(LocalDateTime.now());

        this.updateById(existingAddress);

    }

    /**
     * 删除收货地址（FR-AM-003）
     */
    @Override
    public void deleteAddress(Long addressId, Long userId){
        //1.校验地址归属
        UserAddress existingAddress = this.getById(addressId);
        checkAddressOwnership(existingAddress, userId);

        this.removeById(addressId);

        //TODO:如果删除的是默认地址，则需要考虑是否需要自动将另一条地址设为默认
    }

    /**
     * 设置默认地址（FR-AM-005）
     */
    @Override
    @Transactional
    public void setDefaultAddress(Long addressId, Long userId){
        //1.校验地址归属
        UserAddress existingAddress = this.getById(addressId);
        checkAddressOwnership(existingAddress, userId);

        //文档使用分布式锁，这里暂时用数据库事务

        //2.将所有地址设为非默认
        addressMapper.setAllNotDefault(userId);

        //3.将指定地址设置为默认
        existingAddress.setIsDefault(1);
        existingAddress.setUpdateTime(LocalDateTime.now());
        this.updateById(existingAddress);
    }

}
