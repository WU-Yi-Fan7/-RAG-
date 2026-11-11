package cn.edu.hubu.hubuactivities.service.impl;

import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.entity.Sign;
import cn.edu.hubu.hubuactivities.entity.Student;
import cn.edu.hubu.hubuactivities.mapper.ActivityMapper;
import cn.edu.hubu.hubuactivities.mapper.SignMapper;
import cn.edu.hubu.hubuactivities.mapper.StudentMapper;
import cn.edu.hubu.hubuactivities.service.ActivityService;
import cn.edu.hubu.hubuactivities.vo.ActivityVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final SignMapper signMapper;
    private final StudentMapper studentMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private final DateTimeFormatter simpleDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public ResultVO<List<ActivityVO>> getAllActivities() {
        try {
            log.info("获取所有活动列表");
            List<Activity> activities = activityMapper.getAllPublishedActivities();
            List<ActivityVO> voList = convertToVOList(activities, true);
            return ResultVO.success("获取成功", voList);
        } catch (Exception e) {
            log.error("获取活动列表失败", e);
            return ResultVO.error(500, "获取活动列表失败");
        }
    }

    @Override
    public ResultVO<ActivityVO> getActivityDetail(Long id) {
        try {
            log.info("获取活动详情，ID: {}", id);
            Activity activity = activityMapper.getActivityById(id);

            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            ActivityVO vo = convertToVO(activity, true);
            return ResultVO.success("获取成功", vo);
        } catch (Exception e) {
            log.error("获取活动详情失败", e);
            return ResultVO.error(500, "获取活动详情失败");
        }
    }

    @Override
    public ResultVO<List<ActivityVO>> getActivitiesByType(Integer typeId) {
        try {
            log.info("根据类型获取活动，typeId: {}", typeId);
            List<Activity> activities = activityMapper.getActivitiesByType(typeId);
            List<ActivityVO> voList = convertToVOList(activities, true);
            return ResultVO.success("获取成功", voList);
        } catch (Exception e) {
            log.error("根据类型获取活动失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> signUp(Long activityId, Long studentId) {
        try {
            log.info("学生报名，activityId: {}, studentId: {}", activityId, studentId);

            Activity activity = activityMapper.getActivityById(activityId);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            String currentStatus = calculateActivityStatus(activity);
            if (!"signing".equals(currentStatus)) {
                if ("planning".equals(currentStatus)) {
                    return ResultVO.error(400, "活动尚未开始报名，请耐心等待");
                } else if ("ongoing".equals(currentStatus)) {
                    return ResultVO.error(400, "活动正在进行中，报名已截止");
                } else if ("ended".equals(currentStatus)) {
                    return ResultVO.error(400, "活动已结束，无法报名");
                }
                return ResultVO.error(400, "当前状态不可报名");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getSign_start())) {
                return ResultVO.error(400, "报名尚未开始");
            }
            if (now.isAfter(activity.getSign_end())) {
                return ResultVO.error(400, "报名已截止");
            }

            Integer signCount = activity.getSign_count() != null ? activity.getSign_count() : 0;
            if (signCount >= activity.getTotal()) {
                return ResultVO.error(400, "报名人数已满");
            }

            int validSigned = signMapper.checkValidSigned(activityId, studentId);
            if (validSigned > 0) {
                return ResultVO.error(400, "您已报名该活动");
            }

            Sign existingSign = signMapper.getSignByActivityAndStudent(activityId, studentId);

            if (existingSign != null && "cancelled".equals(existingSign.getStatus())) {
                existingSign.setStatus("pending");
                existingSign.setSignTime(LocalDateTime.now());
                existingSign.setReviewTime(null);
                existingSign.setReviewerId(null);
                existingSign.setReviewRemark(null);
                signMapper.updateById(existingSign);
                log.info("重新报名成功，更新已取消记录，activityId: {}, studentId: {}", activityId, studentId);
            } else {
                Sign sign = new Sign();
                sign.setActivityId(activityId);
                sign.setStudentId(studentId);
                sign.setSignTime(LocalDateTime.now());
                sign.setStatus("pending");
                signMapper.insert(sign);
                log.info("新报名成功，activityId: {}, studentId: {}", activityId, studentId);
            }

            Activity currentActivity = activityMapper.getActivityById(activityId);
            if (currentActivity.getSign_count() < currentActivity.getTotal()) {
                signMapper.incrementSignCount(activityId);
            }

            return ResultVO.success("报名成功", null);

        } catch (Exception e) {
            log.error("报名失败", e);
            return ResultVO.error(500, "报名失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> signUpByStuId(Long activityId, String stuId) {
        try {
            log.info("根据学号报名，activityId: {}, stuId: {}", activityId, stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                log.warn("学生信息不存在，学号: {}", stuId);
                return ResultVO.error(404, "学生信息不存在");
            }

            Long studentId = student.getId();
            return signUp(activityId, studentId);

        } catch (Exception e) {
            log.error("根据学号报名失败", e);
            return ResultVO.error(500, "报名失败：" + e.getMessage());
        }
    }

    @Override
    public ResultVO<Map<String, Object>> checkSigned(Long activityId, String stuId) {
        try {
            log.info("检查报名状态，activityId: {}, stuId: {}", activityId, stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            LambdaQueryWrapper<Sign> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Sign::getActivityId, activityId)
                    .eq(Sign::getStudentId, student.getId());
            Sign sign = signMapper.selectOne(wrapper);

            Map<String, Object> result = new HashMap<>();

            boolean isValidSign = sign != null && !"cancelled".equals(sign.getStatus());
            result.put("signed", isValidSign);

            if (isValidSign && sign != null) {
                Map<String, Object> signInfo = new HashMap<>();
                signInfo.put("status", sign.getStatus());
                signInfo.put("statusText", getSignStatusText(sign.getStatus()));
                signInfo.put("signTime", sign.getSignTime().format(dateFormatter));
                if (sign.getReviewTime() != null) {
                    signInfo.put("reviewTime", sign.getReviewTime().format(dateFormatter));
                }
                if (sign.getReviewRemark() != null) {
                    signInfo.put("reviewRemark", sign.getReviewRemark());
                }
                if (sign.getCheckinTime() != null) {
                    signInfo.put("checkinTime", sign.getCheckinTime().format(dateFormatter));
                }
                result.put("signInfo", signInfo);
            }

            return ResultVO.success("获取成功", result);
        } catch (Exception e) {
            log.error("检查报名状态失败", e);
            return ResultVO.error(500, "检查失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> cancelSign(Long activityId, String stuId) {
        try {
            log.info("取消报名，activityId: {}, stuId: {}", activityId, stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            Activity activity = activityMapper.getActivityById(activityId);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(activity.getStart_time())) {
                return ResultVO.error(400, "活动已开始，无法取消报名");
            }

            Sign sign = signMapper.getSignByActivityAndStudent(activityId, student.getId());
            if (sign == null) {
                return ResultVO.error(404, "未找到报名记录");
            }

            if ("checked".equals(sign.getStatus())) {
                return ResultVO.error(400, "您已签到，无法取消报名");
            }

            if ("cancelled".equals(sign.getStatus())) {
                return ResultVO.error(400, "已经取消过了");
            }

            sign.setStatus("cancelled");
            signMapper.updateById(sign);
            signMapper.decrementSignCount(activityId);

            log.info("取消报名成功，activityId: {}, stuId: {}", activityId, stuId);
            return ResultVO.success("已取消报名", null);

        } catch (Exception e) {
            log.error("取消报名失败", e);
            return ResultVO.error(500, "取消报名失败：" + e.getMessage());
        }
    }

    @Override
    public ResultVO<List<ActivityVO>> getUserActivities(String stuId) {
        try {
            log.info("获取用户报名的活动列表，stuId: {}", stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            List<Activity> activities = signMapper.getUserActivities(student.getId());
            List<ActivityVO> voList = convertToVOList(activities, true);

            return ResultVO.success("获取成功", voList);
        } catch (Exception e) {
            log.error("获取用户活动列表失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    // ========== 管理员审核相关方法 ==========

    @Override
    public ResultVO<List<Map<String, Object>>> getActivitiesWithPendingSigns() {
        try {
            List<Map<String, Object>> activities = signMapper.getActivitiesWithPendingSigns();
            return ResultVO.success("获取成功", activities);
        } catch (Exception e) {
            log.error("获取有待审核报名的活动列表失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    public ResultVO<List<Map<String, Object>>> getPendingStudentsByActivityId(Long activityId) {
        try {
            List<Map<String, Object>> students = signMapper.getPendingStudentsByActivityId(activityId);
            return ResultVO.success("获取成功", students);
        } catch (Exception e) {
            log.error("获取待审核学生列表失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    public ResultVO<List<Map<String, Object>>> getAllStudentsByActivityId(Long activityId) {
        try {
            List<Map<String, Object>> students = signMapper.getAllStudentsByActivityId(activityId);
            return ResultVO.success("获取成功", students);
        } catch (Exception e) {
            log.error("获取报名学生列表失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> reviewSign(Long signId, String status, String remark, Long reviewerId) {
        try {
            log.info("审核报名，signId: {}, status: {}, reviewerId: {}", signId, status, reviewerId);

            Sign sign = signMapper.selectById(signId);
            if (sign == null) {
                return ResultVO.error(404, "报名记录不存在");
            }

            if (!"pending".equals(sign.getStatus())) {
                return ResultVO.error(400, "该报名已审核，请勿重复操作");
            }

            sign.setStatus(status);
            sign.setReviewTime(LocalDateTime.now());
            sign.setReviewerId(reviewerId);
            sign.setReviewRemark(remark);
            signMapper.updateById(sign);

            if ("rejected".equals(status)) {
                signMapper.decrementSignCount(sign.getActivityId());
                log.info("未录取，释放名额，activityId: {}", sign.getActivityId());
            }

            log.info("审核完成，signId: {}, status: {}", signId, status);
            return ResultVO.success(status.equals("approved") ? "已录取" : "已拒绝", null);

        } catch (Exception e) {
            log.error("审核失败", e);
            return ResultVO.error(500, "审核失败：" + e.getMessage());
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> batchReviewSign(List<Long> signIds, String status, String remark, Long reviewerId) {
        try {
            log.info("批量审核报名，signIds: {}, status: {}", signIds, status);

            if (signIds == null || signIds.isEmpty()) {
                return ResultVO.error(400, "请选择要审核的报名");
            }

            int successCount = 0;
            int failCount = 0;
            List<String> failedIds = new ArrayList<>();

            for (Long signId : signIds) {
                try {
                    ResultVO<String> result = reviewSign(signId, status, remark, reviewerId);
                    if (result.getCode() == 200) {
                        successCount++;
                    } else {
                        failCount++;
                        failedIds.add(String.valueOf(signId));
                    }
                } catch (Exception e) {
                    log.error("审核失败，signId: {}", signId, e);
                    failCount++;
                    failedIds.add(String.valueOf(signId));
                }
            }

            String message = "批量审核完成，成功：" + successCount + "，失败：" + failCount;
            if (!failedIds.isEmpty()) {
                message += "，失败ID：" + String.join(",", failedIds);
            }

            log.info(message);
            return ResultVO.success(message, null);

        } catch (Exception e) {
            log.error("批量审核失败", e);
            return ResultVO.error(500, "批量审核失败：" + e.getMessage());
        }
    }
    // ========== 管理员活动管理 ==========
    /**
     * 验证活动时间合法性
     */
    private ResultVO<String> validateActivityTime(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = activity.getStart_time();
        LocalDateTime endTime = activity.getEnd_time();
        LocalDateTime signStart = activity.getSign_start();
        LocalDateTime signEnd = activity.getSign_end();

        // 1. 开始时间不能是过去的时间
        if (startTime.isBefore(now)) {
            return ResultVO.error(400, "活动开始时间不能早于当前时间");
        }

        // 2. 结束时间不能早于开始时间，且至少相差12小时
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return ResultVO.error(400, "活动结束时间必须晚于开始时间");
        }
        long hoursDiff = ChronoUnit.HOURS.between(startTime, endTime);
        if (hoursDiff < 12) {
            return ResultVO.error(400, "活动结束时间至少要比开始时间晚12小时");
        }

        // 3. 报名开始时间不能晚于活动开始时间
        if (signStart.isAfter(startTime) || signStart.equals(startTime)) {
            return ResultVO.error(400, "报名开始时间必须早于活动开始时间");
        }

        // 4. 报名结束时间必须早于活动结束时间，且至少提前24小时
        if (signEnd.isAfter(endTime) || signEnd.equals(endTime)) {
            return ResultVO.error(400, "报名截止时间必须在活动结束时间之前");
        }
        long endTimeDiff = ChronoUnit.HOURS.between(signEnd, endTime);
        if (endTimeDiff < 24) {
            return ResultVO.error(400, "报名截止时间至少要比活动结束时间提前24小时");
        }

        // 5. 报名结束时间不能早于报名开始时间，且至少间隔1天
        if (signEnd.isBefore(signStart) || signEnd.equals(signStart)) {
            return ResultVO.error(400, "报名截止时间必须晚于报名开始时间");
        }
        long signDaysDiff = ChronoUnit.DAYS.between(signStart, signEnd);
        if (signDaysDiff < 1) {
            return ResultVO.error(400, "报名截止时间至少要比报名开始时间晚1天");
        }

        return ResultVO.success("验证通过", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<Activity> createActivity(Activity activity, String publisherStuId) {
        // 在 createActivity 方法开头添加
        ResultVO<String> validateResult = validateActivityTime(activity);
        if (validateResult.getCode() != 200) {
            return ResultVO.error(validateResult.getCode(), validateResult.getMessage());
        }
        try {
            log.info("创建活动，标题: {}", activity.getTitle());

            // 获取发布人
            Student student = studentMapper.findByStuId(publisherStuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            // 设置默认值
            activity.setStatus("published");  // 改为 published，直接发布
            activity.setActivity_status("planning");
            activity.setPublisher_id(student.getId());
            activity.setPublish_time(LocalDateTime.now());
            activity.setView_count(0);
            activity.setSign_count(0);
            activity.setSign_ok(0);
            activity.setRemain(activity.getTotal());

            // 保存
            int result = activityMapper.insert(activity);

            if (result > 0) {
                log.info("活动创建成功，ID: {}", activity.getId());
                return ResultVO.success("创建成功", activity);
            } else {
                return ResultVO.error(500, "创建失败");
            }

        } catch (Exception e) {
            log.error("创建活动失败", e);
            return ResultVO.error(500, "创建失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<Activity> updateActivity(Long id, Activity activity) {
        // 在 updateActivity 方法开头添加
        ResultVO<String> validateResult = validateActivityTime(activity);
        if (validateResult.getCode() != 200) {
            return ResultVO.error(validateResult.getCode(), validateResult.getMessage());
        }
        try {
            log.info("更新活动，ID: {}", id);

            // 查询原活动
            Activity existing = activityMapper.selectById(id);
            if (existing == null) {
                return ResultVO.error(404, "活动不存在");
            }

            // 检查是否可编辑（只有规划中的活动可编辑）
            if (!"planning".equals(existing.getActivity_status())) {
                return ResultVO.error(400, "只有规划中的活动可以编辑");
            }

            // 更新字段
            existing.setTitle(activity.getTitle());
            existing.setContent(activity.getContent());
            existing.setCategory(activity.getCategory());
            existing.setTypeId(activity.getTypeId());
            existing.setCover(activity.getCover());
            existing.setStart_time(activity.getStart_time());
            existing.setEnd_time(activity.getEnd_time());
            existing.setSign_start(activity.getSign_start());
            existing.setSign_end(activity.getSign_end());
            existing.setLocation(activity.getLocation());
            existing.setTotal(activity.getTotal());
            existing.setRemain(activity.getTotal() - existing.getSign_count());
            existing.setCredit_value(activity.getCredit_value());
            existing.setHours_value(activity.getHours_value());

            activityMapper.updateById(existing);

            log.info("活动更新成功，ID: {}", id);
            return ResultVO.success("更新成功", existing);

        } catch (Exception e) {
            log.error("更新活动失败", e);
            return ResultVO.error(500, "更新失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> deleteActivity(Long id) {
        try {
            log.info("删除活动，ID: {}", id);

            Activity activity = activityMapper.selectById(id);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            // 检查是否可删除（规划中和已结束可删除，报名中和进行中不可删除）
            String status = activity.getActivity_status();
            if ("signing".equals(status) || "ongoing".equals(status)) {
                return ResultVO.error(400, "报名中或进行中的活动无法删除");
            }

            // 硬删除：直接从数据库删除
            int result = activityMapper.deleteById(id);

            if (result > 0) {
                log.info("活动删除成功，ID: {}", id);
                return ResultVO.success("删除成功", null);
            } else {
                return ResultVO.error(500, "删除失败");
            }

        } catch (Exception e) {
            log.error("删除活动失败", e);
            return ResultVO.error(500, "删除失败：" + e.getMessage());
        }
    }

    @Override
    public ResultVO<Activity> getActivityForEdit(Long id) {
        try {
            Activity activity = activityMapper.selectById(id);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }
            return ResultVO.success("获取成功", activity);
        } catch (Exception e) {
            log.error("获取活动失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }
    // ========== 签到相关方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<Map<String, String>> generateCheckinCode(Long activityId, String codeType) {
        try {
            log.info("生成签到码，activityId: {}, codeType: {}", activityId, codeType);

            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            // 检查活动是否在进行中
            String currentStatus = calculateActivityStatus(activity);
            if (!"ongoing".equals(currentStatus)) {
                return ResultVO.error(400, "只有进行中的活动才能生成签到码");
            }

            Map<String, String> result = new HashMap<>();

            if ("qr".equals(codeType)) {
                // 如果已经有二维码，直接返回已有的（改为 getQr_code()）
                if (activity.getQr_code() != null && !activity.getQr_code().isEmpty()) {
                    result.put("qrCode", activity.getQr_code());
                    result.put("qrContent", activity.getQr_code());
                    log.info("使用已有二维码签到码，activityId: {}", activityId);
                    return ResultVO.success("获取成功", result);
                }

                // 生成新的二维码内容
                String qrContent = activityId + "_" + UUID.randomUUID().toString().substring(0, 8);
                activity.setQr_code(qrContent);  // 改为 setQr_code()
                activity.setNum_gen_time(LocalDateTime.now());
                activityMapper.updateById(activity);

                result.put("qrCode", qrContent);
                result.put("qrContent", qrContent);
                log.info("生成新二维码签到码，activityId: {}", activityId);
            } else if ("num".equals(codeType)) {
                // 如果已经有数字码，直接返回已有的
                if (activity.getNum_code() != null && !activity.getNum_code().isEmpty()) {
                    result.put("numCode", activity.getNum_code());
                    log.info("使用已有数字签到码，activityId: {}, numCode: {}", activityId, activity.getNum_code());
                    return ResultVO.success("获取成功", result);
                }

                // 生成4位随机数字
                Random random = new Random();
                String numCode = String.format("%04d", random.nextInt(10000));
                activity.setNum_code(numCode);
                activity.setNum_gen_time(LocalDateTime.now());
                activityMapper.updateById(activity);

                result.put("numCode", numCode);
                log.info("生成新数字签到码，activityId: {}, numCode: {}", activityId, numCode);
            } else {
                return ResultVO.error(400, "签到码类型错误");
            }

            return ResultVO.success("生成成功", result);

        } catch (Exception e) {
            log.error("生成签到码失败", e);
            return ResultVO.error(500, "生成失败：" + e.getMessage());
        }
    }
    @Override
    public ResultVO<Map<String, Object>> getActivityCheckinInfo(Long activityId) {
        try {
            log.info("获取活动签到信息，activityId: {}", activityId);

            // 获取已录取未签到的学生列表（返回 Map）
            List<Map<String, Object>> pendingStudents = signMapper.getApprovedNotCheckedStudents(activityId);
            // 获取已签到的学生列表（返回 Map）
            List<Map<String, Object>> checkedStudents = signMapper.getCheckedStudents(activityId);

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", (pendingStudents.size() + checkedStudents.size()));
            result.put("pendingCount", pendingStudents.size());
            result.put("checkedCount", checkedStudents.size());
            result.put("pendingStudents", pendingStudents);
            result.put("checkedStudents", checkedStudents);

            return ResultVO.success("获取成功", result);

        } catch (Exception e) {
            log.error("获取签到信息失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> checkin(Long activityId, String checkinCode, String stuId) {
        try {
            log.info("用户签到，activityId: {}, stuId: {}, code: {}", activityId, stuId, checkinCode);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            Activity activity = activityMapper.getActivityById(activityId);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            // 移除活动状态限制，所有状态的活动都可以签到
            // 只检查活动是否已结束（已结束的活动不能签到）
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(activity.getEnd_time())) {
                return ResultVO.error(400, "活动已结束，无法签到");
            }

            Sign sign = signMapper.getByActivityAndStudent(activityId, student.getId());
            if (sign == null) {
                return ResultVO.error(404, "未找到报名记录");
            }

            if (!"approved".equals(sign.getStatus())) {
                return ResultVO.error(400, "您尚未被录取，无法签到");
            }

            if (sign.getCheckinTime() != null) {
                return ResultVO.error(400, "您已签到过了");
            }
            log.info("===== 签到码验证 =====");
            log.info("传入的签到码: {}", checkinCode);
            log.info("数据库qr_code: {}", activity.getQr_code());
            log.info("数据库num_code: {}", activity.getNum_code());

            // 验证签到码
            boolean isValid = false;

// 检查是否匹配二维码（改为 getQr_code()）
            if (activity.getQr_code() != null && activity.getQr_code().equals(checkinCode)) {
                log.info("二维码匹配成功");
                isValid = true;
            }
// 检查是否匹配数字码
            if (!isValid && activity.getNum_code() != null && activity.getNum_code().equals(checkinCode)) {
                log.info("数字码匹配成功");
                isValid = true;
            }

            if (!isValid) {
                log.warn("签到码无效！传入: {}, 数据库qr: {}, 数据库num: {}",
                        checkinCode, activity.getQr_code(), activity.getNum_code());
                return ResultVO.error(400, "签到码无效");
            }

            int result = signMapper.checkin(sign.getId());
            if (result == 0) {
                return ResultVO.error(500, "签到失败");
            }

            // 增加诚信分（上限100）
            studentMapper.incrementCredit(student.getId());

            log.info("签到成功，activityId: {}, stuId: {}", activityId, stuId);
            return ResultVO.success("签到成功", null);

        } catch (Exception e) {
            log.error("签到失败", e);
            return ResultVO.error(500, "签到失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<String> processEndedActivityCheckins(Long activityId) {
        try {
            log.info("处理活动结束后的签到情况，activityId: {}", activityId);

            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                return ResultVO.error(404, "活动不存在");
            }

            // 获取已录取但未签到的学生（返回的是 Map 列表）
            List<Map<String, Object>> pendingStudents = signMapper.getApprovedNotCheckedStudents(activityId);

            int processedCount = 0;
            for (Map<String, Object> student : pendingStudents) {
                // 从 Map 中获取 student_id
                Object studentIdObj = student.get("student_id");
                Long studentId = null;
                if (studentIdObj instanceof Integer) {
                    studentId = ((Integer) studentIdObj).longValue();
                } else if (studentIdObj instanceof Long) {
                    studentId = (Long) studentIdObj;
                }
                if (studentId != null) {
                    studentMapper.decrementCredit(studentId);
                    processedCount++;
                }
            }

            log.info("活动结束处理完成，activityId: {}, 未签到学生数: {}", activityId, processedCount);
            return ResultVO.success("处理完成，共处理" + processedCount + "名未签到学生", null);

        } catch (Exception e) {
            log.error("处理活动结束签到失败", e);
            return ResultVO.error(500, "处理失败：" + e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========

    private String getSignStatusText(String status) {
        if (status == null) return "已报名";
        switch (status) {
            case "pending": return "待审核";
            case "approved": return "已录取";
            case "rejected": return "未录取";
            case "checked": return "已签到";
            case "cancelled": return "已取消";
            default: return "已报名";
        }
    }

    private String calculateActivityStatus(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime signStart = activity.getSign_start();
        LocalDateTime signEnd = activity.getSign_end();
        LocalDateTime activityStart = activity.getStart_time();
        LocalDateTime activityEnd = activity.getEnd_time();

        if (now.isAfter(activityEnd)) {
            return "ended";
        }
        if (now.isAfter(activityStart) && now.isBefore(activityEnd)) {
            return "ongoing";
        }
        Integer signCount = activity.getSign_count() != null ? activity.getSign_count() : 0;
        if (now.isAfter(signStart) && now.isBefore(signEnd) && signCount < activity.getTotal()) {
            return "signing";
        }
        return "planning";
    }

    private void setButtonProperties(ActivityVO vo, String status) {
        switch (status) {
            case "signing":
                vo.setButtonText("报名活动");
                vo.setCanSign(true);
                vo.setButtonClass("active");
                break;
            case "ongoing":
                vo.setButtonText("进行中");
                vo.setCanSign(false);
                vo.setButtonClass("active");
                break;
            case "planning":
                vo.setButtonText("规划中");
                vo.setCanSign(false);
                vo.setButtonClass("disabled");
                break;
            case "ended":
                vo.setButtonText("已结束");
                vo.setCanSign(false);
                vo.setButtonClass("disabled");
                break;
            default:
                vo.setButtonText("未知");
                vo.setCanSign(false);
                vo.setButtonClass("disabled");
        }
    }

    private String calculateCountdown(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = activity.getEnd_time();

        if (now.isAfter(endTime)) {
            return "活动已结束";
        }

        long days = ChronoUnit.DAYS.between(now, endTime);
        long hours = ChronoUnit.HOURS.between(now, endTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;

        if (days > 0) {
            return days + "天" + hours + "时" + minutes + "分" + seconds + "秒";
        } else if (hours > 0) {
            return hours + "时" + minutes + "分" + seconds + "秒";
        } else if (minutes > 0) {
            return minutes + "分" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }

    private String getStatusDisplay(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "planning": return "规划中";
            case "signing": return "报名中";
            case "ongoing": return "进行中";
            case "ended": return "已结束";
            default: return status;
        }
    }

    private String getScoreDisplay(Activity activity) {
        if (activity.getCredit_value() != null && activity.getCredit_value().compareTo(BigDecimal.ZERO) > 0) {
            return "+" + activity.getCredit_value() + "积分";
        } else if (activity.getHours_value() != null && activity.getHours_value() > 0) {
            return "+" + activity.getHours_value() + "工时";
        }
        return "";
    }

    private ActivityVO convertToVO(Activity activity, boolean isDetail) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setContent(activity.getContent());
        vo.setCategory(activity.getCategory());
        vo.setTypeName(activity.getTypeName());
        vo.setCover(activity.getCover());

        if (activity.getStart_time() != null) {
            vo.setStartTime(activity.getStart_time().format(dateFormatter));
        }
        if (activity.getEnd_time() != null) {
            vo.setEndTime(activity.getEnd_time().format(dateFormatter));
        }

        if (activity.getSign_start() != null) {
            vo.setSignStart(activity.getSign_start().format(dateFormatter));
        }
        if (activity.getSign_end() != null) {
            vo.setSignEnd(activity.getSign_end().format(dateFormatter));
        }

        vo.setLocation(activity.getLocation());
        vo.setTotal(activity.getTotal());
        vo.setRemain(activity.getRemain());
        vo.setSignCount(activity.getSign_count() != null ? activity.getSign_count() : 0);
        vo.setCreditValue(activity.getCredit_value());
        vo.setHoursValue(activity.getHours_value());

        String currentStatus = calculateActivityStatus(activity);
        vo.setActivityStatus(currentStatus);
        vo.setStatusDisplay(getStatusDisplay(currentStatus));

        setButtonProperties(vo, currentStatus);

        vo.setCountdownText(calculateCountdown(activity));
        vo.setScoreDisplay(getScoreDisplay(activity));

        // 报名状态字段（用于我的活动页面）
        vo.setSignStatus(activity.getSign_status());
        if (activity.getSign_time() != null) {
            vo.setSignTime(activity.getSign_time().format(dateFormatter));
        }
        if (activity.getCheckin_time() != null) {
            vo.setCheckinTime(activity.getCheckin_time().format(dateFormatter));
        }
        if (activity.getReview_time() != null) {
            vo.setReviewTime(activity.getReview_time().format(dateFormatter));
        }
        vo.setReviewRemark(activity.getReview_remark());

        return vo;
    }

    private List<ActivityVO> convertToVOList(List<Activity> activities, boolean isDetail) {
        List<ActivityVO> voList = new ArrayList<>();
        for (Activity activity : activities) {
            voList.add(convertToVO(activity, true));
        }
        return voList;
    }
}