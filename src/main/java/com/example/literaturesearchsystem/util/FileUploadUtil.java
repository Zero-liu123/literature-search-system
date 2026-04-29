package com.example.literaturesearchsystem.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class FileUploadUtil {

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 上传文件
     * @param file 上传的文件
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 创建目录（如果不存在）
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建上传目录: {}", uploadPath);
            }

            // 获取原文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String newFileName = UUID.randomUUID().toString() + extension;

            // 保存文件
            String filePath = uploadPath + newFileName;
            file.transferTo(new File(filePath));

            log.info("文件上传成功: {}", filePath);
            return "/uploads/" + newFileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param fileUrl 文件访问路径
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // 将URL转换为文件路径（去掉 /uploads/ 前缀）
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String filePath = uploadPath + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("文件删除成功: {}", filePath);
                } else {
                    log.warn("文件删除失败: {}", filePath);
                }
            } else {
                log.warn("文件不存在: {}", filePath);
            }
        } catch (Exception e) {
            log.error("文件删除异常", e);
        }
    }
}