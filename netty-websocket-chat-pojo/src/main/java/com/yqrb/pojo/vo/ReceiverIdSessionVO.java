package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
//ReceiverId 会话实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiverIdSessionVO {
    private String receiverId; // 唯一会话标识
    private String userId; // 用户ID
    private String userName; // 用户名
    private Date createTime; // 创建时间
    private Date expireTime; // 过期时间
    private Boolean isOnline; // 是否在线
}