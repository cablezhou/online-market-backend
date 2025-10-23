package com.zhoucable.marketbackend.modules.upload.service.impl;


import com.zhoucable.marketbackend.common.BusinessException;
import com.zhoucable.marketbackend.modules.upload.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    //从application.yml注入配置
    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    @Override
    public String uploadImage(MultipartFile file){

        //1.基本校验
        if(file == null || file.isEmpty()){
            throw new BusinessException(4001,"上传的文件不能为空");
        }

        //2.获取原始文件名和后缀
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null){
            throw new BusinessException(4001, "文件名获取失败");
        }
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if(dotIndex >= 0){
            fileExtension = originalFilename.substring(dotIndex); // .jpg .png etc.
        }

        //检查文件类型、后缀是否允许（只允许.jpg .png .jpeg）
        if(!isValidImageType(fileExtension)){
            throw new BusinessException(4001, "不支持的文件类型");
        }

        //生成唯一文件名（UUID+后缀）防止重名覆盖
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + fileExtension;

        //4.创建目标文件路径
        File destDir = new File(uploadPath);
        //确保目录存在，不存在则创建
        if(!destDir.exists()){
            if(!destDir.mkdirs()){
                log.error("创建上传目录失败：{}", uploadPath);
                throw new BusinessException(500, "服务器内部错误，无法创建文件上传目录");
            }
        }
        File destFile = new File(destDir, uniqueFilename);

        //5.保存文件
        try{
            file.transferTo(destFile);
        }catch(IOException e){
            log.error("文件保存失败：{}", destFile.getAbsoluteFile(), e);
            throw new BusinessException(500, "文件上传失败，请稍后再试");
        }

        //6.拼接并返回可访问的URL
        //确保baseUrl以 / 结尾，uniqueFilename不以 / 开头
        String fileUrl = baseUrl.endsWith("/") ? baseUrl + uniqueFilename : baseUrl + "/" + uniqueFilename;
        return fileUrl;


    }

    private boolean isValidImageType(String extension){
        String lowerExt = extension.toLowerCase();
        return lowerExt.equals(".jpg") || lowerExt.equals(".png") || lowerExt.equals(".jpeg");
    }
}
