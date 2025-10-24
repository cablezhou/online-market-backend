package com.zhoucable.marketbackend.modules.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.product.dto.SpecificationDTO;
import com.zhoucable.marketbackend.modules.product.entity.ProductSku;
import com.zhoucable.marketbackend.modules.product.mapper.ProductSkuMapper;
import com.zhoucable.marketbackend.modules.product.service.ProductSkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSkuServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSku> implements ProductSkuService {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String standardizeSpecifications(List<SpecificationDTO> specifications){

        if(specifications.isEmpty() || specifications == null){
            throw new BusinessException(4011, "规格列表为空");
        }

        //1. 按key的ASCII字典序升序排序
        List<SpecificationDTO> sortedSpecs = specifications.stream()
                .sorted(Comparator.comparing(SpecificationDTO::getKey))
                .collect(Collectors.toList());

        //2.序列化为紧凑JSON字符串
        try{
            //使用ObjectMapper生成无空格的JSON
            return objectMapper.writeValueAsString(sortedSpecs);
        }catch (JsonProcessingException e){
            //TODO:记录日志
            throw new BusinessException(500, "规格序列化失败");
        }
    }

    @Override
    public boolean checkDuplicateSku(Long productId, String standardizedSpecJson){

        LambdaQueryWrapper<ProductSku> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ProductSku::getProductId, productId)
                //直接比较JSON字符串
                .eq(ProductSku::getSpecifications, standardizedSpecJson);
        return this.count(queryWrapper) > 0;

    }
}
