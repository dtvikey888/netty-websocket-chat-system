package com.yqrb.pojo;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "newspaper_file_record")
public class NewspaperFileRecord {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 文件唯一标识
     */
    @Column(name = "file_id")
    private String fileId;

    /**
     * 关联登报申请ID
     */
    @Column(name = "app_id")
    private String appId;

    /**
     * 上传用户ID
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 压缩后文件名
     */
    @Column(name = "compress_file_name")
    private String compressFileName;

    /**
     * 压缩后文件存储路径
     */
    @Column(name = "compress_file_path")
    private String compressFilePath;

    /**
     * 压缩后文件大小（字节）
     */
    @Column(name = "compress_file_size")
    private Long compressFileSize;

    /**
     * 文件后缀（jpg/png等）
     */
    @Column(name = "file_ext")
    private String fileExt;

    /**
     * 上传时间
     */
    @Column(name = "upload_time")
    private Date uploadTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 获取主键ID
     *
     * @return id - 主键ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键ID
     *
     * @param id 主键ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取文件唯一标识
     *
     * @return file_id - 文件唯一标识
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * 设置文件唯一标识
     *
     * @param fileId 文件唯一标识
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    /**
     * 获取关联登报申请ID
     *
     * @return app_id - 关联登报申请ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 设置关联登报申请ID
     *
     * @param appId 关联登报申请ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取上传用户ID
     *
     * @return user_id - 上传用户ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置上传用户ID
     *
     * @param userId 上传用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取压缩后文件名
     *
     * @return compress_file_name - 压缩后文件名
     */
    public String getCompressFileName() {
        return compressFileName;
    }

    /**
     * 设置压缩后文件名
     *
     * @param compressFileName 压缩后文件名
     */
    public void setCompressFileName(String compressFileName) {
        this.compressFileName = compressFileName;
    }

    /**
     * 获取压缩后文件存储路径
     *
     * @return compress_file_path - 压缩后文件存储路径
     */
    public String getCompressFilePath() {
        return compressFilePath;
    }

    /**
     * 设置压缩后文件存储路径
     *
     * @param compressFilePath 压缩后文件存储路径
     */
    public void setCompressFilePath(String compressFilePath) {
        this.compressFilePath = compressFilePath;
    }

    /**
     * 获取压缩后文件大小（字节）
     *
     * @return compress_file_size - 压缩后文件大小（字节）
     */
    public Long getCompressFileSize() {
        return compressFileSize;
    }

    /**
     * 设置压缩后文件大小（字节）
     *
     * @param compressFileSize 压缩后文件大小（字节）
     */
    public void setCompressFileSize(Long compressFileSize) {
        this.compressFileSize = compressFileSize;
    }

    /**
     * 获取文件后缀（jpg/png等）
     *
     * @return file_ext - 文件后缀（jpg/png等）
     */
    public String getFileExt() {
        return fileExt;
    }

    /**
     * 设置文件后缀（jpg/png等）
     *
     * @param fileExt 文件后缀（jpg/png等）
     */
    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    /**
     * 获取上传时间
     *
     * @return upload_time - 上传时间
     */
    public Date getUploadTime() {
        return uploadTime;
    }

    /**
     * 设置上传时间
     *
     * @param uploadTime 上传时间
     */
    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    /**
     * 获取创建时间
     *
     * @return create_time - 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间
     *
     * @return update_time - 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}