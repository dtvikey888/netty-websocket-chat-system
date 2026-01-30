package com.yqrb.pojo.query;

import lombok.Data;

/**
 * 离线消息查询参数POJO
 */
@Data
public class OfflineMsgQueryParam {

    /**
     * 客服ID
     */
    private String serviceStaffId;

    /**
     * 是否已推送（0=未推送，1=已推送）
     */
    private Integer isPushed;
}