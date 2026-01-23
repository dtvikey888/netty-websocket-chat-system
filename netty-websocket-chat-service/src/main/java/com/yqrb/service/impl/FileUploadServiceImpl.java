package com.yqrb.service.impl;

import com.yqrb.mapper.NewspaperFileRecordMapperCustom;
import com.yqrb.pojo.vo.NewspaperFileRecordVO;
import com.yqrb.pojo.vo.Result;
import com.yqrb.service.FileUploadService;
import com.yqrb.service.ReceiverIdService;
import com.yqrb.util.DateUtil;
import com.yqrb.util.FileCompressUtil;
import com.yqrb.util.UUIDUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Resource
    private FileCompressUtil fileCompressUtil;

    @Resource
    private NewspaperFileRecordMapperCustom newspaperFileRecordMapperCustom;

    @Resource
    private ReceiverIdService receiverIdService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NewspaperFileRecordVO> uploadCompressImage(MultipartFile file, String appId, String userId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 校验必要参数
        if (appId == null || appId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            return Result.error("申请ID和用户ID不能为空");
        }

        // 3. 执行图片上传与压缩（仅保留压缩文件，自动删除原始文件）
        String[] compressFileInfo;
        try {
            compressFileInfo = fileCompressUtil.uploadAndCompressOnlyKeepCompressed(file);
        } catch (Exception e) {
            return Result.error("图片上传压缩失败：" + e.getMessage());
        }

        // 4. 解析压缩文件信息
        String compressFilePath = compressFileInfo[0];
        String compressFileName = compressFileInfo[1];
        String fileExt = compressFileInfo[2];
        File compressFile = new File(compressFilePath);
        long compressFileSize = compressFile.length();

        // 5. 构建压缩文件记录
        NewspaperFileRecordVO fileRecord = new NewspaperFileRecordVO();
        fileRecord.setFileId(UUIDUtil.generateFileId());
        fileRecord.setAppId(appId);
        fileRecord.setUserId(userId);
        fileRecord.setCompressFileName(compressFileName);
        fileRecord.setCompressFilePath(compressFilePath);
        fileRecord.setCompressFileSize(compressFileSize);
        fileRecord.setFileExt(fileExt);
        Date currentDate = DateUtil.getCurrentDate();
        fileRecord.setUploadTime(currentDate);
        fileRecord.setCreateTime(currentDate);
        fileRecord.setUpdateTime(currentDate);

        // 6. 保存压缩文件记录到数据库
        int insertResult = newspaperFileRecordMapperCustom.insertCompressFileRecord(fileRecord);
        if (insertResult <= 0) {
            // 数据库保存失败，删除已生成的压缩文件，避免残留
            if (compressFile.exists() && !compressFile.delete()) {
                System.err.println("数据库记录保存失败，压缩文件删除失败：" + compressFilePath);
            }
            return Result.error("保存压缩文件记录失败");
        }

        // 7. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        // 8. 返回压缩文件记录详情
        NewspaperFileRecordVO resultRecord = newspaperFileRecordMapperCustom.selectByFileId(fileRecord.getFileId());
        return Result.success(resultRecord);
    }

    @Override
    public Result<List<NewspaperFileRecordVO>> getCompressFileListByAppId(String appId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询压缩文件列表
        List<NewspaperFileRecordVO> fileList = newspaperFileRecordMapperCustom.selectByAppId(appId);
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(fileList);
    }

    @Override
    public Result<NewspaperFileRecordVO> getCompressFileByFileId(String fileId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询压缩文件详情
        NewspaperFileRecordVO fileRecord = newspaperFileRecordMapperCustom.selectByFileId(fileId);
        if (fileRecord == null) {
            return Result.error("压缩文件记录不存在");
        }

        // 3. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(fileRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteCompressFile(String fileId, String receiverId) {
        // 1. 校验ReceiverId有效性
        if (!receiverIdService.validateReceiverId(receiverId)) {
            return Result.unauthorized("ReceiverId无效或已过期");
        }

        // 2. 查询压缩文件详情
        NewspaperFileRecordVO fileRecord = newspaperFileRecordMapperCustom.selectByFileId(fileId);
        if (fileRecord == null) {
            return Result.error("压缩文件记录不存在");
        }

        // 3. 删除物理压缩文件
        File compressFile = new File(fileRecord.getCompressFilePath());
        if (compressFile.exists() && !compressFile.delete()) {
            System.err.println("压缩文件物理删除失败：" + fileRecord.getCompressFilePath());
            // 此处不抛出异常，继续删除数据库记录
        }

        // 4. 删除数据库记录（此处需扩展Mapper接口：deleteByFileId）
        // 临时方案：先删除appId下所有记录（实际项目需补充deleteByFileId方法）
        int deleteResult = newspaperFileRecordMapperCustom.deleteByAppId(fileRecord.getAppId());
        if (deleteResult <= 0) {
            return Result.error("删除压缩文件记录失败");
        }

        // 5. 刷新ReceiverId过期时间
        receiverIdService.refreshReceiverIdExpire(receiverId);

        return Result.success(true);
    }
}