package cn.edu.hubu.hubuactivities.mapper;

import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.entity.Sign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SignMapper extends BaseMapper<Sign> {

    @Select("SELECT COUNT(*) FROM activity_sign WHERE activity_id = #{activityId} AND student_id = #{studentId}")
    int checkStudentSigned(@Param("activityId") Long activityId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM activity_sign WHERE activity_id = #{activityId} AND student_id = #{studentId} AND status != 'cancelled'")
    int checkValidSigned(@Param("activityId") Long activityId, @Param("studentId") Long studentId);

    @Update("UPDATE activity SET sign_count = sign_count + 1, remain = remain - 1 WHERE id = #{activityId} AND sign_count < total")
    int incrementSignCount(@Param("activityId") Long activityId);

    @Update("UPDATE activity SET sign_count = sign_count - 1, remain = remain + 1 WHERE id = #{activityId}")
    int decrementSignCount(@Param("activityId") Long activityId);

    @Select("SELECT COUNT(*) FROM activity_sign WHERE activity_id = #{activityId} AND status = 'approved'")
    int getApprovedCountByActivityId(@Param("activityId") Long activityId);

    // 获取用户报名记录
    @Select("SELECT * FROM activity_sign WHERE activity_id = #{activityId} AND student_id = #{studentId}")
    Sign getSignByActivityAndStudent(@Param("activityId") Long activityId, @Param("studentId") Long studentId);

    // 获取用户报名的活动列表（排除已取消的）
    @Select("SELECT a.*, t.type_name, " +
            "s.status as sign_status, " +
            "s.sign_time, s.checkin_time, s.review_time, s.review_remark " +
            "FROM activity_sign s " +
            "LEFT JOIN activity a ON s.activity_id = a.id " +
            "LEFT JOIN activity_type t ON a.type_id = t.id " +
            "WHERE s.student_id = #{studentId} AND s.status != 'cancelled' " +
            "ORDER BY s.sign_time DESC")
    List<Activity> getUserActivities(@Param("studentId") Long studentId);

    // ========== 管理员审核相关 ==========

    // 获取所有有待审核报名的活动
    @Select("SELECT DISTINCT a.id, a.title, a.start_time, a.end_time, a.location, " +
            "(SELECT COUNT(*) FROM activity_sign WHERE activity_id = a.id AND status = 'pending') as pending_count " +
            "FROM activity a " +
            "INNER JOIN activity_sign s ON a.id = s.activity_id " +
            "WHERE s.status = 'pending' AND a.status = 'published' " +
            "ORDER BY a.start_time ASC")
    List<Map<String, Object>> getActivitiesWithPendingSigns();

    // 获取某个活动的待审核报名学生列表
    @Select("SELECT s.id as sign_id, s.student_id, s.sign_time, s.status, " +
            "stu.real_name, stu.stu_id, stu.department, stu.major, stu.phone, stu.grade " +
            "FROM activity_sign s " +
            "LEFT JOIN student stu ON s.student_id = stu.id " +
            "WHERE s.activity_id = #{activityId} AND s.status = 'pending' " +
            "ORDER BY s.sign_time ASC")
    List<Map<String, Object>> getPendingStudentsByActivityId(@Param("activityId") Long activityId);

    // 获取某个活动的所有报名学生（管理员查看）
    @Select("SELECT s.id as sign_id, s.student_id, s.sign_time, s.checkin_time, s.review_time, s.review_remark, s.status, " +
            "stu.real_name, stu.stu_id, stu.department, stu.major, stu.phone, stu.grade " +
            "FROM activity_sign s " +
            "LEFT JOIN student stu ON s.student_id = stu.id " +
            "WHERE s.activity_id = #{activityId} AND s.status != 'cancelled' " +
            "ORDER BY s.sign_time DESC")
    List<Map<String, Object>> getAllStudentsByActivityId(@Param("activityId") Long activityId);
    // 获取活动已录取未签到的学生列表 - 返回 Map 类型
    @Select("SELECT s.id, s.student_id, s.sign_time, s.checkin_time, s.status, " +
            "stu.real_name, stu.stu_id, stu.department, stu.major, stu.phone " +
            "FROM activity_sign s " +
            "LEFT JOIN student stu ON s.student_id = stu.id " +
            "WHERE s.activity_id = #{activityId} AND s.status = 'approved' AND s.checkin_time IS NULL")
    List<Map<String, Object>> getApprovedNotCheckedStudents(@Param("activityId") Long activityId);

    // 获取活动已签到的学生列表 - 返回 Map 类型
    @Select("SELECT s.id, s.student_id, s.sign_time, s.checkin_time, s.status, " +
            "stu.real_name, stu.stu_id, stu.department, stu.major, stu.phone " +
            "FROM activity_sign s " +
            "LEFT JOIN student stu ON s.student_id = stu.id " +
            "WHERE s.activity_id = #{activityId} AND s.checkin_time IS NOT NULL")
    List<Map<String, Object>> getCheckedStudents(@Param("activityId") Long activityId);
    // 签到
    @Update("UPDATE activity_sign SET checkin_time = NOW(), status = 'checked' WHERE id = #{signId}")
    int checkin(@Param("signId") Long signId);

    // 根据学生ID和活动ID获取报名记录
    @Select("SELECT * FROM activity_sign WHERE activity_id = #{activityId} AND student_id = #{studentId}")
    Sign getByActivityAndStudent(@Param("activityId") Long activityId, @Param("studentId") Long studentId);

    // 获取待审核报名总数
    @Select("SELECT COUNT(*) FROM activity_sign WHERE status = 'pending'")
    long getPendingCount();

    // 获取已签到总数
    @Select("SELECT COUNT(*) FROM activity_sign WHERE checkin_time IS NOT NULL")
    long getCheckedCount();
}