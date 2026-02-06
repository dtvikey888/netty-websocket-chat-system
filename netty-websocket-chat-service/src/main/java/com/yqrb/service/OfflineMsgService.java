package com.yqrb.service;

import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.OfflineMsgVO;

import java.util.List;

/**
 * 离线消息Service接口
 * 改造：支持sessionId关联，实现指定会话的存储/查询/标记推送
 */
public interface OfflineMsgService {

    /**
     * 存储离线消息（指定会话，客服/用户离线时调用）
     * @param offlineMsg 离线消息对象（含sessionId）
     * @return 存储是否成功
     */
    boolean saveOfflineMsg(OfflineMsgVO offlineMsg);

    /**
     * 多条件查询离线消息（支持按「接收者ID+会话ID+推送状态」精准查询）
     * @param queryParam 查询参数（含sessionId）
     * @return 离线消息列表
     */
    List<OfflineMsgVO> getOfflineMsgList(OfflineMsgQueryParam queryParam);

    /**
     * 标记指定「接收者ID+会话ID」的离线消息为「已推送」（精准标记，避免跨会话）
     * @param serviceStaffId 接收者ID（用户/客服）
     * @param sessionId 业务会话ID
     * @return 标记是否成功
     */
    boolean markOfflineMsgAsPushed(String serviceStaffId, String sessionId);
}