package com.yqrb.mapper;

import com.yqrb.pojo.vo.SessionMappingVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionMappingMapperCustom {

    // 新增会话映射
    @Insert("INSERT INTO session_mapping (session_id, app_id, user_id, service_staff_id, create_time, update_time) " +
            "VALUES (#{sessionId}, #{appId}, #{userId}, #{serviceStaffId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSessionMapping(SessionMappingVO sessionMapping);

    // 根据sessionId查询会话映射
    @Select("SELECT * FROM session_mapping WHERE session_id = #{sessionId}")
    SessionMappingVO selectBySessionId(String sessionId);

    // 根据appId查询会话映射
    @Select("SELECT * FROM session_mapping WHERE app_id = #{appId}")
    SessionMappingVO selectByAppId(String appId);

    // 根据用户ID查询会话列表
    @Select("SELECT * FROM session_mapping WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<SessionMappingVO> selectByUserId(String userId);

    // 删除会话映射（按sessionId）
    int deleteBySessionId(String sessionId);
}
