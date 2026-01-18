package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
//会话映射表实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionMappingVO {
    private Long id; // 主键ID
    private String sessionId; // 会话唯一标识
    private String appId; // 关联登报申请ID
    private String userId; // 用户ID
    private String serviceStaffId; // 客服ID
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间
}