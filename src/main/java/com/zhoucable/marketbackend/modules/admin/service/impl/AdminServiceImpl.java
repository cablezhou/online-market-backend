package com.zhoucable.marketbackend.modules.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.admin.dto.AdminReviewDTO;
import com.zhoucable.marketbackend.modules.admin.dto.ReviewActionType;
import com.zhoucable.marketbackend.modules.admin.service.AdminService;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;
import com.zhoucable.marketbackend.modules.merchant.mapper.MerchantApplicationMapper;
import com.zhoucable.marketbackend.modules.merchant.service.MerchantService;
import com.zhoucable.marketbackend.modules.merchant.service.StoreService;
import com.zhoucable.marketbackend.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AdminServiceImpl extends ServiceImpl<MerchantApplicationMapper, MerchantApplication> implements AdminService {

    @Autowired
    private UserService userService;

    @Autowired
    private StoreService storeService;

    //MerchantApplicationMapper已由ServiceImpl自动注入

    @Override
    @Transactional //确保多表操作的原子性
    public void reviewApplication(Long applicationId, AdminReviewDTO reviewDTO){

        //1.查找申请单
        MerchantApplication application = this.getById(applicationId);
        if(application == null){
            throw new BusinessException(4042, "未找到申请");
        }

        //2.检查状态，只有“待审核”才可以继续操作
        if(!Objects.equals(application.getStatus(), 0)){
            throw new BusinessException(4094, "该审核已被处理，请勿重复操作");
        }

        if(ReviewActionType.APPROVE.equals(reviewDTO.getAction())){

            //3. ----审核通过----
            //3.1 更新状态单
            application.setStatus(1); //已通过
            application.setUpdateTime(LocalDateTime.now());
            this.updateById(application);

            //3.2 更改用户角色为商家
            // FR-UM-004: 审核通过后，该用户的角色（role）字段更新为“商家”
            userService.changeUserRole(application.getUserId(), 1);

            //3.3 为其创建店铺
            // FR-UM-004 并为其创建一个默认店铺
            storeService.createStore(
                    application.getUserId(),
                    application.getStoreName(),
                    application.getDescription()
            );
        } else if (ReviewActionType.REJECT.equals(reviewDTO.getAction())) {

            //4. ----审核拒绝----
            application.setStatus(2); //已拒绝
            application.setReason(reviewDTO.getReason());
            application.setUpdateTime(LocalDateTime.now());
            this.updateById(application);
        }else {
            throw new BusinessException(4001, "无效的操作类型");
        }
    }
}
