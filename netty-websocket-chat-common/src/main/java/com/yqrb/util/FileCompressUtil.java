package com.yqrb.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
public class FileCompressUtil {

    @Value("${custom.file.upload.base-path}")
    private String basePath;

    @Value("${custom.file.upload.compress.width}")
    private Integer compressWidth;

    @Value("${custom.file.upload.compress.quality}")
    private Float compressQuality;

    @Value("${custom.file.upload.compress.suffix}")
    private String compressSuffix;

    /**
     * 上传图片并压缩（仅保留压缩文件，自动删除原始文件）
     * @param file 上传的图片文件
     * @return 压缩文件相关信息：[压缩文件路径, 压缩文件名, 文件后缀]
     * @throws IOException 文件操作异常
     */
    public String[] uploadAndCompressOnlyKeepCompressed(MultipartFile file) throws IOException {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 2. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("无效的图片文件，缺少文件后缀");
        }
        String fileExt = StrUtil.subAfter(originalFilename, ".", true).toLowerCase();
        if (!"jpg,png,jpeg,bmp,gif".contains(fileExt)) {
            throw new RuntimeException("不支持的图片格式，仅支持jpg/png/jpeg/bmp/gif");
        }

        // 3. 创建日期目录（按yyyyMMdd分目录存储，避免文件过多）
        String dateDir = DateUtil.formatDate(new Date(), "yyyyMMdd");
        String fullDatePath = basePath + dateDir + "/";
        File dateDirFile = new File(fullDatePath);
        if (!dateDirFile.exists() && !dateDirFile.mkdirs()) {
            throw new RuntimeException("创建压缩文件存储目录失败：" + fullDatePath);
        }

        // 4. 生成唯一文件前缀（避免文件名冲突）
        String filePrefix = UUIDUtil.getUUID();
        String originalFileName = filePrefix + "." + fileExt;
        String originalFilePath = fullDatePath + originalFileName;
        File originalFile = new File(originalFilePath);

        // 5. 保存原始文件（临时存储，用于生成压缩文件）
        file.transferTo(originalFile);

        // 6. 生成压缩文件信息
        String compressFileName = filePrefix + compressSuffix + "." + fileExt;
        String compressFilePath = fullDatePath + compressFileName;
        File compressFile = new File(compressFilePath);

        // 7. 执行图片压缩（强制压缩，无开关）
        try {
            Thumbnails.of(originalFile)
                    .width(compressWidth)
                    .outputQuality(compressQuality)
                    .toFile(compressFile);
        } catch (IOException e) {
            // 压缩失败时，删除临时原始文件，避免残留
            if (originalFile.exists() && !originalFile.delete()) {
                System.err.println("压缩失败，临时原始文件删除失败：" + originalFilePath);
            }
            throw new RuntimeException("图片压缩失败：" + e.getMessage(), e);
        }

        // 8. 压缩成功后，删除原始文件（核心：仅保留压缩文件）
        if (originalFile.exists()) {
            boolean isDeleted = originalFile.delete();
            if (!isDeleted) {
                System.err.println("压缩文件生成成功，但原始文件删除失败：" + originalFilePath);
                // 此处不抛出异常，避免影响主流程（仅打印日志，确保压缩文件可用）
            }
        }

        // 9. 校验压缩文件是否生成成功
        if (!compressFile.exists()) {
            throw new RuntimeException("压缩文件生成失败，无有效压缩文件输出");
        }

        // 10. 返回压缩文件核心信息（压缩路径、压缩文件名、文件后缀）
        return new String[]{compressFilePath, compressFileName, fileExt};
    }
}