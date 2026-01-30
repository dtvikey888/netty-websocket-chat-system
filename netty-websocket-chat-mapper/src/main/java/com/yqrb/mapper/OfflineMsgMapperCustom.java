package com.yqrb.mapper;

import com.yqrb.pojo.query.OfflineMsgQueryParam;
import org.apache.ibatis.annotations.Param;
import com.yqrb.pojo.vo.OfflineMsgVO;

import java.util.List;

public interface OfflineMsgMapperCustom  {
    /**
     * 多条件查询离线消息（核心：查询指定客服的未推送消息）
     * @param queryParam 查询参数
     * @return 离线消息列表
     */
    List<OfflineMsgVO> selectByCondition(OfflineMsgQueryParam queryParam);

    /**
     * 批量更新指定客服的消息为「已推送」
     * @param serviceStaffId 客服ID
     * @param isPushed 推送状态（1=已推送）
     * @return 受影响行数
     */
    int updateIsPushedByServiceStaffId(@Param("serviceStaffId") String serviceStaffId, @Param("isPushed") Integer isPushed);

    /**
     * 清理过期已推送的离线消息
     * @param days 过期天数
     * @return 清理行数
     */
    int cleanExpiredOfflineMsg(@Param("days") Integer days);


    /**
     * 新增：插入离线消息（对应XML的insert语句）
     * @param offlineMsgVO 离线消息VO对象
     * @return 受影响行数（1=插入成功，0=插入失败）
     */
    int insertOfflineMsg(OfflineMsgVO offlineMsgVO);
}