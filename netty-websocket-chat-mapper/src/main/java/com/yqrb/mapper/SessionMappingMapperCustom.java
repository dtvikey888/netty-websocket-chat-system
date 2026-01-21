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

    // 删除会话映射（按sessionId）—— 已与XML中的SQL映射，无需修改
    int deleteBySessionId(String sessionId);

    // ========== 新增缺失方法 ==========
    /**
     * 按客服ID查询承接的所有会话（客服的会话列表）
     */
    @Select("SELECT * FROM session_mapping WHERE service_staff_id = #{serviceStaffId} ORDER BY update_time DESC")
    List<SessionMappingVO> selectByServiceStaffId(String serviceStaffId);

    /**
     * 按用户ID+申请ID查询会话（避免重复创建）
     */
    @Select("SELECT * FROM session_mapping WHERE user_id = #{userId} AND app_id = #{appId}")
    SessionMappingVO selectByUserIdAndAppId(SessionMappingVO sessionMapping);

    /**
     * 更新会话映射（如更换客服、刷新更新时间）
     */
    int updateSessionMapping(SessionMappingVO sessionMapping);

    /**
     * 按申请ID删除会话映射（申请作废/完成后清理）
     */
    int deleteByAppId(String appId);
}
