package com.yqrb.enums;


/**
 * 登报申请状态枚举（纯内存存储，无数据库关联）
 */
public enum ApplicationStatusEnum {
    /** 待审核 */
    PENDING("待审核"),
    /** 审核通过（待付款） */
    PASS("待付款"),
    /** 审核驳回 */
    REJECT("已驳回");

    private final String desc;

    ApplicationStatusEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
