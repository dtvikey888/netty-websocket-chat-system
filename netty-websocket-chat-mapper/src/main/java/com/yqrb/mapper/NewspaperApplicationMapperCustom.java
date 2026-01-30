package com.yqrb.mapper;

import com.yqrb.pojo.vo.NewspaperApplicationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete; // 新增：导入Delete注解

import java.util.List;

public interface NewspaperApplicationMapperCustom {

    // 新增登报申请（原有，无需修改）
    @Insert("INSERT INTO newspaper_application (app_id, user_id, service_staff_id, user_name, user_phone, " +
            "cert_type, seal_re, pay_amount, status, submit_time, create_time, update_time) " +
            "VALUES (#{appId}, #{userId}, #{serviceStaffId}, #{userName}, #{userPhone}, " +
            "#{certType}, #{sealRe}, #{payAmount}, #{status}, #{submitTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNewspaperApplication(NewspaperApplicationVO application);

    // 根据appId查询登报申请（原有，无需修改）
    @Select("SELECT * FROM newspaper_application WHERE app_id = #{appId}")
    NewspaperApplicationVO selectByAppId(String appId);

    // 根据用户ID查询登报申请列表（原有，无需修改）
    @Select("SELECT * FROM newspaper_application WHERE user_id = #{userId} ORDER BY submit_time DESC")
    List<NewspaperApplicationVO> selectByUserId(String userId);

    // 根据客服ID查询登报申请列表（原有，无需修改）
    @Select("SELECT * FROM newspaper_application WHERE service_staff_id = #{serviceStaffId} ORDER BY submit_time DESC")
    List<NewspaperApplicationVO> selectByServiceStaffId(String serviceStaffId);

    // 更新登报申请状态（审核/支付/驳回/退款）
    // ========== 核心修改：添加退款相关字段的更新 ==========
    @Update("UPDATE newspaper_application SET " +
            "status = #{status}, " +
            "audit_remark = #{auditRemark}, " +
            "audit_time = #{auditTime}, " +
            "pay_time = #{payTime}, " +
            "pay_amount= #{payAmount}, " +
            "refund_amount = #{refundAmount}, " + // 新增：退款金额
            "refund_apply_time = #{refundApplyTime}, " + // 新增：退款申请时间
            "refund_remark = #{refundRemark}, " + // 新增：退款备注
            "update_time = #{updateTime} " +
            "WHERE app_id = #{appId}")
    int updateStatusByAppId(NewspaperApplicationVO application);

    // 删除登报申请（按appId）
    // ========== 小修改：将@Update改为@Delete，语义更准确 ==========
    @Delete("DELETE FROM newspaper_application WHERE app_id = #{appId}")
    int deleteByAppId(String appId);
}