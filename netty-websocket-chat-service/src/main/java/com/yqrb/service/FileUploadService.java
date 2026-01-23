package com.yqrb.service;

import com.yqrb.pojo.vo.NewspaperFileRecordVO;
import com.yqrb.pojo.vo.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {
    // 上传图片（仅保留压缩文件，自动删除原始文件）
    Result<NewspaperFileRecordVO> uploadCompressImage(MultipartFile file, String appId, String userId, String receiverId);

    // 根据appId查询压缩图片列表
    Result<List<NewspaperFileRecordVO>> getCompressFileListByAppId(String appId, String receiverId);

    // 根据fileId查询压缩图片详情
    Result<NewspaperFileRecordVO> getCompressFileByFileId(String fileId, String receiverId);

    // 删除压缩图片（按fileId）
    Result<Boolean> deleteCompressFile(String fileId, String receiverId);
}