package com.yqrb.service;

import com.yqrb.pojo.vo.Result;
import com.yqrb.pojo.vo.SessionMappingVO;

import java.util.List;

public interface SessionMappingService {
    // 原有方法
    Result<SessionMappingVO> createSessionMapping(SessionMappingVO sessionMapping, String receiverId);
    Result<SessionMappingVO> getBySessionId(String sessionId, String receiverId);
    Result<SessionMappingVO> getByAppId(String appId, String receiverId);
    Result<List<SessionMappingVO>> getByUserId(String userId, String receiverId);
    Result<Boolean> deleteBySessionId(String sessionId, String receiverId);

    // 新增缺失方法
    /**
     * 按客服ID查询承接的会话列表
     */
    Result<List<SessionMappingVO>> getByServiceStaffId(String serviceStaffId, String receiverId);

    /**
     * 按用户+申请ID查询会话（避免重复创建）
     */
    Result<SessionMappingVO> getByUserIdAndAppId(String userId, String appId, String receiverId);

    /**
     * 更新会话映射（如更换客服）
     */
    Result<Boolean> updateSessionMapping(SessionMappingVO sessionMapping, String receiverId);

    /**
     * 按申请ID删除会话映射
     */
    Result<Boolean> deleteByAppId(String appId, String receiverId);
}