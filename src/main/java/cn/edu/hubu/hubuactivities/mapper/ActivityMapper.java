package cn.edu.hubu.hubuactivities.mapper;

import cn.edu.hubu.hubuactivities.entity.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    @Select("SELECT a.*, t.type_name " +
            "FROM activity a " +
            "LEFT JOIN activity_type t ON a.type_id = t.id " +
            "WHERE a.status = 'published' " +
            "ORDER BY a.publish_time DESC")
    IPage<Activity> selectActivityPage(Page<?> page);

    @Select("SELECT * " +
            "FROM activity a " +
            "INNER JOIN activity_type t ON a.type_id = t.id")
    List<Activity> getAllActivities();

    @Select("SELECT a.*, t.type_name " +
            "FROM activity a " +
            "LEFT JOIN activity_type t ON a.type_id = t.id " +
            "WHERE a.status = 'published' " +
            "ORDER BY a.publish_time DESC")
    List<Activity> getAllPublishedActivities();

    @Select("SELECT a.*, t.type_name " +
            "FROM activity a " +
            "LEFT JOIN activity_type t ON a.type_id = t.id " +
            "WHERE a.id = #{id}")
    Activity getActivityById(@Param("id") Long id);

    @Select("SELECT a.*, t.type_name " +
            "FROM activity a " +
            "LEFT JOIN activity_type t ON a.type_id = t.id " +
            "WHERE a.status = 'published' AND a.type_id = #{typeId} " +
            "ORDER BY a.publish_time DESC")
    List<Activity> getActivitiesByType(@Param("typeId") Integer typeId);

    // 统计进行中的活动数（基于当前时间动态计算）
    @Select("SELECT COUNT(*) FROM activity WHERE status = 'published' " +
            "AND start_time <= NOW() AND end_time >= NOW()")
    long getOngoingCount();

    // 统计今日活动数（基于当前时间）
    @Select("SELECT COUNT(*) FROM activity WHERE status = 'published' " +
            "AND DATE(start_time) = CURDATE()")
    long getTodayActivitiesCount();

    // 统计平均签到率（基于实际报名和签到数据）
    @Select("SELECT COALESCE(AVG(CASE WHEN sign_count > 0 THEN sign_ok * 100.0 / sign_count ELSE 0 END), 0) " +
            "FROM activity WHERE status = 'published'")
    double getAverageSignRate();

    // 获取各活动报名趋势数据（只显示报名中的活动）
    @Select("SELECT a.id, a.title, a.start_time, a.sign_count, " +
            "COALESCE(COUNT(s.id), 0) as sign_ok, " +
            "ROUND(COALESCE(COUNT(s.id) * 100.0 / NULLIF(a.sign_count, 0), 0), 2) as sign_rate " +
            "FROM activity a " +
            "LEFT JOIN activity_sign s ON a.id = s.activity_id AND s.checkin_time IS NOT NULL " +
            "WHERE a.status = 'published' AND a.activity_status = 'signing' " +
            "GROUP BY a.id, a.title, a.start_time, a.sign_count " +
            "ORDER BY a.start_time ASC LIMIT 10")
    List<Map<String, Object>> getActivitySignTrend();

    // 获取各类型活动统计
    @Select("SELECT t.type_name, COUNT(a.id) as count, COALESCE(SUM(a.sign_count), 0) as total_signs " +
            "FROM activity_type t " +
            "LEFT JOIN activity a ON t.id = a.type_id AND a.status = 'published' " +
            "GROUP BY t.id ORDER BY t.sort_order")
    List<Map<String, Object>> getCategoryStats();
}