package com.yqrb.mapper;

import com.yqrb.pojo.query.OfflineMsgQueryParam;
import org.apache.ibatis.annotations.Param;
import com.yqrb.pojo.vo.OfflineMsgVO;

import java.util.List;

public interface OfflineMsgMapperCustom  {
    /**
     * 多条件查询离线消息（新增：支持sessionId过滤，精准查询指定会话）
     * @param queryParam 查询参数（含serviceStaffId+sessionId+isPushed）
     * @return 离线消息列表
     */
    List<OfflineMsgVO> selectByCondition(OfflineMsgQueryParam queryParam);

    /**
     * 批量更新指定接收者+会话的消息为「已推送」（优化：精准更新，避免跨会话）
     * @param serviceStaffId 接收者ID
     * @param sessionId 业务会话ID
     * @param isPushed 推送状态（1=已推送）
     * @return 受影响行数
     */
    int updateIsPushedByServiceStaffIdAndSessionId(
            @Param("serviceStaffId") String serviceStaffId,
            @Param("sessionId") String sessionId,
            @Param("isPushed") Integer isPushed
    );

    /**
     * 清理过期已推送的离线消息
     * @param days 过期天数
     * @return 清理行数
     */
    int cleanExpiredOfflineMsg(@Param("days") Integer days);

    /**
     * 插入离线消息（新增：支持sessionId插入）
     * @param offlineMsgVO 离线消息VO对象（含sessionId）
     * @return 受影响行数（1=插入成功，0=插入失败）
     */
    int insertOfflineMsg(OfflineMsgVO offlineMsgVO);
}