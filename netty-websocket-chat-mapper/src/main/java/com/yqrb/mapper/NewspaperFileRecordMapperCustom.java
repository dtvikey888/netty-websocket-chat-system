package com.yqrb.mapper;

import com.yqrb.pojo.vo.NewspaperFileRecordVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewspaperFileRecordMapperCustom {

    // 保存压缩图片上传记录（无原始文件相关字段）
    @Insert("INSERT INTO newspaper_file_record (file_id, app_id, user_id, compress_file_name, " +
            "compress_file_path, compress_file_size, file_ext, upload_time, create_time, update_time) " +
            "VALUES (#{fileId}, #{appId}, #{userId}, #{compressFileName}, #{compressFilePath}, " +
            "#{compressFileSize}, #{fileExt}, #{uploadTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCompressFileRecord(NewspaperFileRecordVO fileRecord);

    // 根据appId查询压缩图片记录列表
    @Select("SELECT * FROM newspaper_file_record WHERE app_id = #{appId} ORDER BY upload_time DESC")
    List<NewspaperFileRecordVO> selectByAppId(String appId);

    // 根据fileId查询压缩图片记录
    @Select("SELECT * FROM newspaper_file_record WHERE file_id = #{fileId}")
    NewspaperFileRecordVO selectByFileId(String fileId);

    // 根据用户ID查询压缩图片记录列表
    @Select("SELECT * FROM newspaper_file_record WHERE user_id = #{userId} ORDER BY upload_time DESC")
    List<NewspaperFileRecordVO> selectByUserId(String userId);

    // 批量删除压缩文件记录（按appId，XML实现）
    int deleteByAppId(String appId);
}