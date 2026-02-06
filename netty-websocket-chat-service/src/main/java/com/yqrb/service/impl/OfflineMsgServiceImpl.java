package com.yqrb.service.impl;

import com.yqrb.mapper.OfflineMsgMapperCustom;
import com.yqrb.pojo.query.OfflineMsgQueryParam;
import com.yqrb.pojo.vo.OfflineMsgVO;
import com.yqrb.service.OfflineMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 离线消息Service实现类
 * 改造：sessionId全链路支持，精准操作指定会话的离线消息
 */
@Slf4j
@Service
public class OfflineMsgServiceImpl implements OfflineMsgService {

    @Autowired
    private OfflineMsgMapperCustom offlineMsgMapperCustom;

    /**
     * 存储离线消息（指定会话，客服/用户离线时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOfflineMsg(OfflineMsgVO offlineMsgVO) {
        try {
            // 补全默认值
            if (offlineMsgVO.getIsPushed() == null) {
                offlineMsgVO.setIsPushed(0); // 默认未推送
            }
            // 插入（含sessionId）
            int insertCount = offlineMsgMapperCustom.insertOfflineMsg(offlineMsgVO);
            boolean result = insertCount > 0;
            if (result) {
                // 日志新增sessionId，便于调试
                log.info("【离线消息存储成功】接收者ID：{}，会话ID：{}，申请ID：{}",
                        offlineMsgVO.getServiceStaffId(), offlineMsgVO.getSessionId(), offlineMsgVO.getAppId());
            } else {
                log.error("【离线消息存储失败】接收者ID：{}，会话ID：{}，申请ID：{}",
                        offlineMsgVO.getServiceStaffId(), offlineMsgVO.getSessionId(), offlineMsgVO.getAppId());
            }
            return result;
        } catch (Exception e) {
            log.error("【离线消息存储异常】接收者ID：{}，会话ID：{}，申请ID：{}，异常信息：{}",
                    offlineMsgVO.getServiceStaffId(), offlineMsgVO.getSessionId(), offlineMsgVO.getAppId(),
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * 多条件查询离线消息（支持按「接收者ID+会话ID+推送状态」精准查询）
     */
    @Override
    public List<OfflineMsgVO> getOfflineMsgList(OfflineMsgQueryParam queryParam) {
        try {
            List<OfflineMsgVO> offlineMsgList = offlineMsgMapperCustom.selectByCondition(queryParam);
            if (CollectionUtils.isEmpty(offlineMsgList)) {
                log.info("【离线消息查询】无符合条件的消息，查询参数：{}", queryParam);
            } else {
                log.info("【离线消息查询成功】接收者ID：{}，会话ID：{}，共查询到 {} 条离线消息",
                        queryParam.getServiceStaffId(), queryParam.getSessionId(), offlineMsgList.size());
            }
            return offlineMsgList;
        } catch (Exception e) {
            log.error("【离线消息查询异常】查询参数：{}，异常信息：{}", queryParam, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 标记指定「接收者ID+会话ID」的离线消息为「已推送」（精准标记，避免跨会话）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean markOfflineMsgAsPushed(String serviceStaffId, String sessionId) {
        try {
            int updateCount = offlineMsgMapperCustom.updateIsPushedByServiceStaffIdAndSessionId(
                    serviceStaffId, sessionId, 1
            );
            boolean result = updateCount >= 0; // 无消息更新时返回0，也算成功
            if (result) {
                log.info("【离线消息标记成功】接收者ID：{}，会话ID：{}，共标记 {} 条消息为已推送",
                        serviceStaffId, sessionId, updateCount);
            } else {
                log.error("【离线消息标记失败】接收者ID：{}，会话ID：{}", serviceStaffId, sessionId);
            }
            return result;
        } catch (Exception e) {
            log.error("【离线消息标记异常】接收者ID：{}，会话ID：{}，异常信息：{}",
                    serviceStaffId, sessionId, e.getMessage(), e);
            return false;
        }
    }
}