package cn.edu.hubu.hubuactivities.service;

import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.entity.Sign;
import cn.edu.hubu.hubuactivities.vo.ActivityVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import java.util.List;
import java.util.Map;

public interface ActivityService {

    ResultVO<List<ActivityVO>> getAllActivities();

    ResultVO<ActivityVO> getActivityDetail(Long id);

    ResultVO<List<ActivityVO>> getActivitiesByType(Integer typeId);

    ResultVO<String> signUp(Long activityId, Long studentId);

    ResultVO<String> signUpByStuId(Long activityId, String stuId);

    ResultVO<Map<String, Object>> checkSigned(Long activityId, String stuId);

    ResultVO<String> cancelSign(Long activityId, String stuId);

    ResultVO<List<ActivityVO>> getUserActivities(String stuId);

    // ========== 管理员审核相关 ==========

    // 获取有待审核报名的活动列表
    ResultVO<List<Map<String, Object>>> getActivitiesWithPendingSigns();

    // 获取某个活动的待审核学生列表
    ResultVO<List<Map<String, Object>>> getPendingStudentsByActivityId(Long activityId);

    // 获取某个活动的所有报名学生
    ResultVO<List<Map<String, Object>>> getAllStudentsByActivityId(Long activityId);

    // 审核报名
    ResultVO<String> reviewSign(Long signId, String status, String remark, Long reviewerId);
    // 添加批量审核接口
    ResultVO<String> batchReviewSign(List<Long> signIds, String status, String remark, Long reviewerId);
    // 在 ActivityService.java 中添加以下方法

    // ========== 管理员活动管理 ==========
    ResultVO<Activity> createActivity(Activity activity, String publisherStuId);
    ResultVO<Activity> updateActivity(Long id, Activity activity);
    ResultVO<String> deleteActivity(Long id);
    ResultVO<Activity> getActivityForEdit(Long id);
    // ========== 签到相关 ==========

    // 管理员生成签到码
    ResultVO<Map<String, String>> generateCheckinCode(Long activityId, String codeType);

    ResultVO<Map<String, Object>> getActivityCheckinInfo(Long activityId);

    // 用户签到（扫码或输入数字）
    ResultVO<String> checkin(Long activityId, String checkinCode, String stuId);

    // 活动结束后批量处理未签到学生（扣诚信分）
    ResultVO<String> processEndedActivityCheckins(Long activityId);
}