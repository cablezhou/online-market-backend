package com.zhoucable.marketbackend.modules.address.controller;


import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.address.dto.AddressDTO;
import com.zhoucable.marketbackend.modules.address.service.AddressService;
import com.zhoucable.marketbackend.modules.address.vo.AddressVO;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.BitSet;
import java.util.List;

/**
 * 地址管理控制器
 * @author 周开播
 * @Date 2025年10月28日16:50:42
 */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * (FR-AM-001) 添加收货地址
     */
    @PostMapping
    public Result<Void> addAddress(@Valid @RequestBody AddressDTO addressDTO){
        Long userId = BaseContext.getCurrentId();
        addressService.addAddress(addressDTO, userId);
        return Result.success();
    }

    /**
     * (FR-AM-004) 查看收货地址列表
     */
    @GetMapping
    public Result<List<AddressVO>> getAddressList(){
        Long userId = BaseContext.getCurrentId();
        List<AddressVO> list = addressService.getAddressList(userId);
        return Result.success(list);
    }

    /**
     * (FR-AM-002) 修改收货地址
     */
    @PutMapping("/{addressId}")
    public Result<Void> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO addressDTO
    ){
        Long userId = BaseContext.getCurrentId();
        addressService.updateAddress(addressId, addressDTO, userId);
        return Result.success();
    }

    /**
     * (FR-AM-003) 删除收货地址
     */
    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(
            @PathVariable Long addressId
    ){
        Long userId = BaseContext.getCurrentId();
        addressService.deleteAddress(addressId, userId);
        return Result.success();
    }

    /**
     * (FR-AM-005) 设置默认地址
     */
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefaultAddress(
            @PathVariable Long addressId
    ){
        Long userId = BaseContext.getCurrentId();
        addressService.setDefaultAddress(addressId, userId);
        return Result.success();
    }

}
