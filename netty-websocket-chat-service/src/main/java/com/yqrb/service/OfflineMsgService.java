package com.yqrb.service;

import com.yqrb.pojo.OfflineMsg;
import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.OfflineMsgVO;

import java.util.List;

/**
 * 离线消息Service接口
 */
public interface OfflineMsgService {

    /**
     * 存储离线消息（客服离线时调用）
     * @param offlineMsg 离线消息对象
     * @return 存储是否成功
     */
    boolean saveOfflineMsg(OfflineMsgVO offlineMsg);

    /**
     * 多条件查询离线消息
     * @param queryParam 查询参数
     * @return 离线消息列表
     */
    List<OfflineMsgVO> getOfflineMsgList(OfflineMsgQueryParam queryParam);

    /**
     * 标记指定客服的离线消息为「已推送」
     * @param serviceStaffId 客服ID
     * @return 标记是否成功
     */
    boolean markOfflineMsgAsPushed(String serviceStaffId);
}