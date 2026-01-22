package com.yqrb.mapper;

import com.yqrb.pojo.vo.SessionMappingVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface SessionMappingMapperCustom {

    // 新增会话映射（保留注解）
    @Insert("INSERT INTO session_mapping (session_id, app_id, user_id, service_staff_id, create_time, update_time) " +
            "VALUES (#{sessionId}, #{appId}, #{userId}, #{serviceStaffId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSessionMapping(SessionMappingVO sessionMapping);

    // 复用XML的resultMap，避免字段映射问题
    @Select("SELECT * FROM session_mapping WHERE session_id = #{sessionId}")
    @ResultMap("com.yqrb.mapper.SessionMappingMapperCustom.SessionMappingResultMap")
    SessionMappingVO selectBySessionId(String sessionId);

    @Select("SELECT * FROM session_mapping WHERE app_id = #{appId}")
    @ResultMap("com.yqrb.mapper.SessionMappingMapperCustom.SessionMappingResultMap")
    SessionMappingVO selectByAppId(String appId);

    @Select("SELECT * FROM session_mapping WHERE user_id = #{userId} ORDER BY create_time DESC")
    @ResultMap("com.yqrb.mapper.SessionMappingMapperCustom.SessionMappingResultMap")
    List<SessionMappingVO> selectByUserId(String userId);

    // 无注解，依赖XML（保留）
    int deleteBySessionId(String sessionId);

    @Select("SELECT * FROM session_mapping WHERE service_staff_id = #{serviceStaffId} ORDER BY update_time DESC")
    @ResultMap("com.yqrb.mapper.SessionMappingMapperCustom.SessionMappingResultMap")
    List<SessionMappingVO> selectByServiceStaffId(String serviceStaffId);

    @Select("SELECT * FROM session_mapping WHERE user_id = #{userId} AND app_id = #{appId}")
    @ResultMap("com.yqrb.mapper.SessionMappingMapperCustom.SessionMappingResultMap")
    SessionMappingVO selectByUserIdAndAppId(SessionMappingVO sessionMapping);

    // 无注解，依赖XML（保留）
    int updateSessionMapping(SessionMappingVO sessionMapping);

    // 无注解，依赖XML（保留）
    int deleteByAppId(String appId);
}