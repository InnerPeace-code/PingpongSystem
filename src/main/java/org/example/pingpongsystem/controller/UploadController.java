package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.Utility;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    // 上传用户头像（所有角色通用）
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, Utility.AvatarPath);
    }

    // 上传教练照片（仅教练使用）
    @PostMapping("/coach-photo")
    public Result<String> uploadCoachPhoto(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, Utility.CoachPhotoPath);
    }

    // 通用文件上传逻辑
    private Result<String> uploadFile(MultipartFile file, String uploadDir) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return Result.error(500, "文件为空");
            }
            if (!file.getContentType().startsWith("image/")) {
                return Result.error(500, "请上传图片文件");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + extension;

            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // 返回文件名（仅文件名，不含路径）
            return Result.success(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "文件上传失败");
        }
    }
}