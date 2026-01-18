package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperFileRecordVO {
    private Long id; // 主键ID
    private String fileId; // 文件唯一标识
    private String appId; // 关联登报申请ID
    private String userId; // 上传用户ID
    private String compressFileName; // 压缩后文件名（必填）
    private String compressFilePath; // 压缩后文件存储路径（必填）
    private Long compressFileSize; // 压缩后文件大小（字节，必填）
    private String fileExt; // 文件后缀（jpg/png等）
    private Date uploadTime; // 上传时间
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}