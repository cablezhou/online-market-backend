package com.zhoucable.marketbackend.modules.upload.controller;

import com.zhoucable.marketbackend.common.Result;
import com.zhoucable.marketbackend.modules.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传的控制类
 * @author 周开播
 * @Date 2025年10月23日15:15:53
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private UploadService uploadService;


    /**
     * 通用图片上传接口（商品管理文档功能点3.9）
     * @param file 前端上传的文件，参数名需要为 “file”（或与前端约定一致）
     * @return 包含图片URL的结果
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file){

        String url = uploadService.uploadImage(file);

        Map<String, String> data = new HashMap<>();
        data.put("url", url);

        return Result.success(data);
    }
}
