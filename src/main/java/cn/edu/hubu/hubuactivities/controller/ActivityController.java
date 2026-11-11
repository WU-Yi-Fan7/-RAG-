package cn.edu.hubu.hubuactivities.controller;

import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.service.ActivityService;
import cn.edu.hubu.hubuactivities.vo.ActivityVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 获取所有活动列表
     */
    @GetMapping("/list")
    public ResultVO<List<ActivityVO>> getActivityList() {
        log.info("收到获取活动列表请求");
        return activityService.getAllActivities();
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/detail/{id}")
    public ResultVO<ActivityVO> getActivityDetail(@PathVariable Long id) {
        log.info("收到获取活动详情请求，ID: {}", id);
        return activityService.getActivityDetail(id);
    }

    /**
     * 根据类型获取活动
     */
    @GetMapping("/type/{typeId}")
    public ResultVO<List<ActivityVO>> getActivitiesByType(@PathVariable Integer typeId) {
        log.info("收到根据类型获取活动请求，typeId: {}", typeId);
        return activityService.getActivitiesByType(typeId);
    }

    /**
     * 报名活动
     */
    @PostMapping("/sign/{activityId}")
    public ResultVO<String> signUp(@PathVariable Long activityId) {
        log.info("收到报名请求，activityId: {}", activityId);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String stuId = (String) principal;

        if (stuId == null) {
            log.warn("报名失败：用户未登录");
            return ResultVO.error(401, "请先登录");
        }

        log.info("学生报名，stuId: {}, activityId: {}", stuId, activityId);
        return activityService.signUpByStuId(activityId, stuId);
    }

    /**
     * 检查用户是否已报名
     */
    @GetMapping("/check-signed/{activityId}")
    public ResultVO<Map<String, Object>> checkSigned(@PathVariable Long activityId) {
        log.info("检查报名状态，activityId: {}", activityId);
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return activityService.checkSigned(activityId, stuId);
    }

    /**
     * 取消报名
     */
    @PostMapping("/cancel/{activityId}")
    public ResultVO<String> cancelSign(@PathVariable Long activityId) {
        log.info("收到取消报名请求，activityId: {}", activityId);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String stuId = (String) principal;
        if (stuId == null) {
            return ResultVO.error(401, "请先登录");
        }
        return activityService.cancelSign(activityId, stuId);
    }

    /**
     * 获取用户报名的活动列表
     */
    @GetMapping("/my-activities")
    public ResultVO<List<ActivityVO>> getUserActivities() {
        log.info("获取用户报名的活动列表");
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String stuId = (String) principal;
        if (stuId == null) {
            return ResultVO.error(401, "请先登录");
        }
        return activityService.getUserActivities(stuId);
    }

    // ========== 管理员审核接口 ==========

    /**
     * 获取有待审核报名的活动列表
     */
    @GetMapping("/admin/pending-activities")
    public ResultVO<List<Map<String, Object>>> getActivitiesWithPendingSigns() {
        log.info("获取有待审核报名的活动列表");
        return activityService.getActivitiesWithPendingSigns();
    }

    /**
     * 获取某个活动的待审核学生列表
     */
    @GetMapping("/admin/pending-students/{activityId}")
    public ResultVO<List<Map<String, Object>>> getPendingStudentsByActivityId(@PathVariable Long activityId) {
        log.info("获取待审核学生列表，activityId: {}", activityId);
        return activityService.getPendingStudentsByActivityId(activityId);
    }

    /**
     * 获取某个活动的所有报名学生
     */
    @GetMapping("/admin/all-students/{activityId}")
    public ResultVO<List<Map<String, Object>>> getAllStudentsByActivityId(@PathVariable Long activityId) {
        log.info("获取报名学生列表，activityId: {}", activityId);
        return activityService.getAllStudentsByActivityId(activityId);
    }

    /**
     * 审核报名
     */
    @PostMapping("/admin/review/{signId}")
    public ResultVO<String> reviewSign(@PathVariable Long signId,
                                       @RequestParam String status,
                                       @RequestParam(required = false) String remark) {
        log.info("审核报名，signId: {}, status: {}", signId, status);
        Long reviewerId = 1L;
        return activityService.reviewSign(signId, status, remark, reviewerId);
    }
    /**
     * 批量审核报名
     */
    @PostMapping("/admin/review/batch")
    public ResultVO<String> batchReviewSign(@RequestBody Map<String, Object> params) {
        log.info("批量审核报名");

        // 处理 signIds 的类型转换（前端传的是 Integer，需要转为 Long）
        List<Long> signIds = new ArrayList<>();
        Object signIdsObj = params.get("signIds");

        if (signIdsObj instanceof List) {
            List<?> tempList = (List<?>) signIdsObj;
            for (Object item : tempList) {
                if (item instanceof Integer) {
                    signIds.add(((Integer) item).longValue());
                } else if (item instanceof Long) {
                    signIds.add((Long) item);
                } else if (item instanceof String) {
                    signIds.add(Long.parseLong((String) item));
                }
            }
        }

        String status = (String) params.get("status");
        String remark = (String) params.get("remark");
        Long reviewerId = 1L;

        if (signIds.isEmpty()) {
            return ResultVO.error(400, "请选择要审核的报名");
        }

        log.info("批量审核，signIds: {}, status: {}", signIds, status);

        return activityService.batchReviewSign(signIds, status, remark, reviewerId);
    }
    // ========== 管理员活动管理接口 ==========

    /**
     * 创建活动
     */
    @PostMapping("/admin/create")
    public ResultVO<Activity> createActivity(@RequestBody Activity activity) {
        log.info("创建活动，标题: {}", activity.getTitle());
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return activityService.createActivity(activity, stuId);
    }

    /**
     * 更新活动
     */
    @PutMapping("/admin/update/{id}")
    public ResultVO<Activity> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        log.info("更新活动，ID: {}", id);
        return activityService.updateActivity(id, activity);
    }

    /**
     * 删除活动
     */
    @DeleteMapping("/admin/delete/{id}")
    public ResultVO<String> deleteActivity(@PathVariable Long id) {
        log.info("删除活动，ID: {}", id);
        return activityService.deleteActivity(id);
    }

    /**
     * 获取单个活动详情（管理员编辑用）
     */
    @GetMapping("/admin/detail/{id}")
    public ResultVO<Activity> getActivityForEdit(@PathVariable Long id) {
        log.info("获取活动详情用于编辑，ID: {}", id);
        return activityService.getActivityForEdit(id);
    }
    // ========== 签到接口 ==========

    /**
     * 管理员生成签到码
     */
    @PostMapping("/admin/generate-checkin-code")
    public ResultVO<Map<String, String>> generateCheckinCode(@RequestParam Long activityId,
                                                             @RequestParam String codeType) {
        log.info("生成签到码，activityId: {}, codeType: {}", activityId, codeType);
        return activityService.generateCheckinCode(activityId, codeType);
    }

    /**
     * 获取活动签到信息
     */
    @GetMapping("/admin/checkin-info/{activityId}")
    public ResultVO<Map<String, Object>> getActivityCheckinInfo(@PathVariable Long activityId) {
        log.info("获取活动签到信息，activityId: {}", activityId);
        return activityService.getActivityCheckinInfo(activityId);
    }

    /**
     * 用户签到
     */
    @PostMapping("/checkin")
    public ResultVO<String> checkin(@RequestParam Long activityId,
                                    @RequestParam String checkinCode) {
        log.info("用户签到，activityId: {}, checkinCode: {}", activityId, checkinCode);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String stuId = (String) principal;
        if (stuId == null) {
            return ResultVO.error(401, "请先登录");
        }
        return activityService.checkin(activityId, checkinCode, stuId);
    }
}