package com.zhoucable.marketbackend.modules.upload.service;


import org.springframework.web.multipart.MultipartFile;

/**
 * 通用的处理图片上传服务类
 * @author 周开播
 * @Date 2025年10月23日14:39:32
 */
public interface UploadService {

    /**
     * 处理图片上次
     * @param file 上传的文件对象
     * @return 可访问的文件url
     */
    String uploadImage(MultipartFile file);

}
