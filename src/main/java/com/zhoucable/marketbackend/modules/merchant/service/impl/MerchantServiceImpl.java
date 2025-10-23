package com.zhoucable.marketbackend.modules.merchant.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.merchant.dto.MerchantApplyDTO;
import com.zhoucable.marketbackend.modules.merchant.entity.MerchantApplication;
import com.zhoucable.marketbackend.modules.merchant.mapper.MerchantApplicationMapper;
import com.zhoucable.marketbackend.modules.merchant.service.MerchantService;
import com.zhoucable.marketbackend.utils.BaseContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantApplicationMapper, MerchantApplication> implements MerchantService {

    @Override
    public void apply(MerchantApplyDTO applyDTO){

        //1.获取当前登录的用户id
        Long userId = BaseContext.getCurrentId();
        if(userId == null){
            //理论上拦截器会处理，这里双重保险
            throw new BusinessException(4012, "请先登录");
        }

        //2.构建申请对象
        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setStoreName(applyDTO.getStoreName());
        application.setDescription(applyDTO.getDescription());
        application.setStatus(0); //初始状态为待审核
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());

        this.save(application);
    }
}
