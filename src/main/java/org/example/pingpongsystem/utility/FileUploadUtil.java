package org.example.pingpongsystem.utility;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUploadUtil {
    // 定义头像存储根路径（可在配置文件中配置，这里简化处理）
    //public static final String AVATAR_BASE_PATH = "uploads/avatars/";

    /**
     * 上传头像并返回文件路径
     */
    public static Result<String> uploadAvatar(MultipartFile file) throws IOException {
        // 1. 验证文件
        if (file.isEmpty()) {
            return Result.error(StatusCode.FAIL, "上传失败：文件为空");
        }
        if (!file.getContentType().startsWith("image/")) {
            return Result.error(StatusCode.FAIL, "上传失败：请上传图片文件");
        }

        // 2. 生成唯一文件名（避免冲突）
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + extension;

        // 3. 确保上传目录存在
        Path uploadPath = Paths.get(Utility.AvatarPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 4. 保存图片到文件系统
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // 5. 返回文件名（存储到数据库）
        return Result.success(fileName);
    }
}