package com.zhoucable.marketbackend.modules.merchant.controller;


import com.zhoucable.marketbackend.common.PageResult;
import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.merchant.dto.MerchantApplyDTO;
import com.zhoucable.marketbackend.modules.merchant.dto.StoreCreateDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.Store;
import com.zhoucable.marketbackend.modules.merchant.service.MerchantService;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.order.dto.OrderListQueryDTO;
import com.zhoucable.marketbackend.modules.order.dto.RefundApproveDTO;
import com.zhoucable.marketbackend.modules.order.dto.ShipOrderDTO;
import com.zhoucable.marketbackend.modules.order.service.OrderService;
import com.zhoucable.marketbackend.modules.order.vo.OrderListVO;
import com.zhoucable.marketbackend.utils.BaseContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家相关的服务层
 * @author 周开播
 * @Date 2025/10/22 16:57
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private OrderService orderService;

    /**
     * 用户提交成为商家的申请
     * @param applyDTO 申请信息DTO
     * @return 操作结果（不是审核结果）
     */
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody MerchantApplyDTO applyDTO){
        merchantService.apply(applyDTO);
        return Result.success();
    }

    /**
     * 商家主动创建新店铺
     * @param createDTO 店铺创建信息 DTO
     * @return 创建成功的店铺信息
     */
    @PostMapping("/stores")
    public Result<Store> createStore(@Valid @RequestBody StoreCreateDTO createDTO){
        Store createdStore = storeService.createStoreByMerchant(createDTO);
        return Result.success(createdStore);
    }

    /**
     * 查询当前商家拥有的店铺列表
     * @return 店铺列表
     * @author 周开播
     * @Date 2025年10月27日15:06:57
     */
    @GetMapping("/stores/my-list")
    public Result<List<Store>> listMyStores(){
        List<Store> storeList = storeService.listStoresByCurrentUser();
        return Result.success(storeList);
    }


    /**
     * 商家发货（FR-OM-006）
     * @param subOrderNumber 子订单号
     * @param shipOrderDTO 物流信息DTO
     * @return 操作结果
     */
    @PostMapping("/orders/{subOrderNumber}/ship")
    public Result<Void> shipOrder(
            @PathVariable String subOrderNumber,
            @Valid @RequestBody ShipOrderDTO shipOrderDTO
            ){
        Long merchantUserId = BaseContext.getCurrentId(); //获取商家用户id
        orderService.shipOrder(merchantUserId,subOrderNumber,shipOrderDTO);
        return Result.success();
    }


    /**
     * 商家查询店铺订单列表 (FR-OM-009)
     * @param storeId 店铺ID
     * @param queryDTO 分页及状态筛选参数
     * @return 订单列表分页结果
     */
    @GetMapping("/stores/{storeId}/orders")
    public Result<PageResult<OrderListVO>> getMerchantOrderList(
            @PathVariable Long storeId,
            OrderListQueryDTO queryDTO
    ){
        Long merchantUserId = BaseContext.getCurrentId();
        PageResult<OrderListVO> pageResult = orderService.getMerchantOrderList(merchantUserId,storeId,queryDTO);
        return Result.success(pageResult);
    }


    /**
     * 商家审核退款申请 (FR-OM-008)
     * @param subOrderNumber 子订单号
     * @param approveDTO 审核操作 (APPROVE/REJECT) 和原因
     * @return 操作结果
     */
    @PutMapping("/orders/{subOrderNumber}/refund-approval")
    public Result<Void> approveRefund(
            @PathVariable String subOrderNumber,
            @Valid @RequestBody RefundApproveDTO approveDTO
            ){
        Long merchantUserId = BaseContext.getCurrentId();
        orderService.approveRefund(merchantUserId, subOrderNumber, approveDTO);
        return Result.success();
    }


}
