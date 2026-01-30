package com.yqrb.service.impl;

import com.yqrb.mapper.OfflineMsgMapper;
import com.yqrb.mapper.OfflineMsgMapperCustom;
import com.yqrb.pojo.OfflineMsg;
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
 */
@Slf4j
@Service
public class OfflineMsgServiceImpl implements OfflineMsgService {

    // 注入Mapper
    @Autowired
    private OfflineMsgMapperCustom offlineMsgMapperCustom;

    /**
     * 存储离线消息（客服离线时调用）
     */
    @Transactional(rollbackFor = Exception.class) // 事务注解：异常时回滚
    @Override
    public boolean saveOfflineMsg(OfflineMsgVO offlineMsgVO) {
        try {
            // 补全默认值
            if (offlineMsgVO.getIsPushed() == null) {
                offlineMsgVO.setIsPushed(0); // 默认未推送
            }
            // tk.mybatis 插入方法
            int insertCount = offlineMsgMapperCustom.insertOfflineMsg(offlineMsgVO);
            boolean result = insertCount > 0;
            if (result) {
                log.info("【离线消息存储成功】客服ID：{}，申请ID：{}", offlineMsgVO.getServiceStaffId(), offlineMsgVO.getAppId());
            } else {
                log.error("【离线消息存储失败】客服ID：{}，申请ID：{}", offlineMsgVO.getServiceStaffId(), offlineMsgVO.getAppId());
            }
            return result;
        } catch (Exception e) {
            log.error("【离线消息存储异常】客服ID：{}，申请ID：{}，异常信息：{}",
                    offlineMsgVO.getServiceStaffId(), offlineMsgVO.getAppId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 多条件查询离线消息
     */
    @Override
    public List<OfflineMsgVO> getOfflineMsgList(OfflineMsgQueryParam queryParam) {
        try {
            List<OfflineMsgVO> offlineMsgList = offlineMsgMapperCustom.selectByCondition(queryParam);
            if (CollectionUtils.isEmpty(offlineMsgList)) {
                log.info("【离线消息查询】无符合条件的消息，查询参数：{}", queryParam);
            } else {
                log.info("【离线消息查询成功】共查询到 {} 条离线消息", offlineMsgList.size());
            }
            return offlineMsgList;
        } catch (Exception e) {
            log.error("【离线消息查询异常】查询参数：{}，异常信息：{}", queryParam, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 标记指定客服的离线消息为「已推送」
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean markOfflineMsgAsPushed(String serviceStaffId) {
        try {
            int updateCount = offlineMsgMapperCustom.updateIsPushedByServiceStaffId(serviceStaffId, 1);
            boolean result = updateCount >= 0; // 无消息更新时返回0，也算成功
            if (result) {
                log.info("【离线消息标记成功】客服ID：{}，共标记 {} 条消息为已推送", serviceStaffId, updateCount);
            } else {
                log.error("【离线消息标记失败】客服ID：{}", serviceStaffId);
            }
            return result;
        } catch (Exception e) {
            log.error("【离线消息标记异常】客服ID：{}，异常信息：{}", serviceStaffId, e.getMessage(), e);
            return false;
        }
    }
}