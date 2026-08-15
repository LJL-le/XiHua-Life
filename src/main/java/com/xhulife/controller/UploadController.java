package com.xhulife.controller;

import cn.hutool.core.util.StrUtil;
import com.xhulife.dto.Result;
import com.xhulife.utils.SystemConstants;
import com.xhulife.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_SUFFIXES = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "webp")
    );

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            if (image == null || image.isEmpty() || image.getSize() > MAX_IMAGE_SIZE) {
                return Result.fail("图片不能为空且不能超过5MB");
            }
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            String suffix = StrUtil.subAfter(originalFilename, ".", true).toLowerCase();
            if (!ALLOWED_SUFFIXES.contains(suffix)) {
                return Result.fail("仅支持 jpg、jpeg、png、webp 图片");
            }
            // 生成新文件名
            String fileName = createNewFileName(originalFilename);
            // 保存文件
            Path target = resolveImagePath(fileName);
            Files.createDirectories(target.getParent());
            image.transferTo(target.toFile());
            // 返回结果
            log.debug("文件上传成功，{}", fileName);
            return Result.ok("uploads/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @DeleteMapping("/blog")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        try {
            Path file = resolveImagePath(filename);
            if (Files.isDirectory(file)) {
                return Result.fail("错误的文件名");
            }
            Files.deleteIfExists(file);
            return Result.ok();
        } catch (IOException | IllegalArgumentException e) {
            return Result.fail("删除图片失败");
        }
    }

    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        if (UserHolder.getUser() == null) throw new IllegalArgumentException("请先登录");
        return StrUtil.format("{}/{}/{}/{}.{}", UserHolder.getUser().getId(), d1, d2, name, suffix);
    }

    private Path resolveImagePath(String filename) {
        if (filename != null) filename = filename.replace('\\', '/').replaceFirst("^/?imgs/uploads/", "").replaceFirst("^uploads/", "");
        if (UserHolder.getUser() == null || filename == null ||
                !filename.startsWith(UserHolder.getUser().getId() + "/")) {
            throw new IllegalArgumentException("无权操作该图片");
        }
        Path root = Paths.get(SystemConstants.IMAGE_UPLOAD_DIR).toAbsolutePath().normalize();
        Path target = root.resolve(filename.replace('\\', '/')).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }
}

