package com.yqrb.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
//客服信息表实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceVO {
    private Long id; // 主键ID
    private String serviceStaffId; // 客服唯一标识（cs_xxx）
    private String serviceName; // 客服姓名
    private String servicePhone; // 客服电话
    private String status; // 在线状态
    private Date loginTime; // 最后登录时间
    private Date createTime; // 创建时间
    private Date updateTime; // 更新时间

    // 客服状态枚举
    public static final String STATUS_ONLINE = "ONLINE"; // 在线
    public static final String STATUS_OFFLINE = "OFFLINE"; // 离线
}