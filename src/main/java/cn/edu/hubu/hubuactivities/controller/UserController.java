package cn.edu.hubu.hubuactivities.controller;

import cn.edu.hubu.hubuactivities.entity.Student;
import cn.edu.hubu.hubuactivities.mapper.ActivityMapper;
import cn.edu.hubu.hubuactivities.mapper.SignMapper;
import cn.edu.hubu.hubuactivities.mapper.StudentMapper;
import cn.edu.hubu.hubuactivities.service.UserService;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import cn.edu.hubu.hubuactivities.vo.UserStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StudentMapper studentMapper;
    private final ActivityMapper activityMapper;
    private final SignMapper signMapper;

    @GetMapping("/stats")
    public ResultVO<UserStatsVO> getUserStats() {
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("获取用户统计数据，学号: {}", stuId);
        return userService.getUserStats(stuId);
    }

    @GetMapping("/student-info")
    public ResultVO<Student> getStudentInfo() {
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("获取学生详细信息，学号: {}", stuId);
        return userService.getStudentInfo(stuId);
    }

    @PostMapping("/change-password")
    public ResultVO<?> changePassword(@RequestBody Map<String, String> passwordMap) {
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String oldPassword = passwordMap.get("oldPassword");
        String newPassword = passwordMap.get("newPassword");
        log.info("修改密码请求，学号: {}", stuId);
        return userService.changePassword(stuId, oldPassword, newPassword);
    }

    // ========== 管理员接口 ==========

    @GetMapping("/admin/info")
    public ResultVO<Map<String, Object>> getAdminInfo() {
        String stuId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("获取管理员信息，学号: {}", stuId);

        Student student = studentMapper.findByStuId(stuId);
        Map<String, Object> info = new HashMap<>();
        info.put("realName", student.getReal_name());
        info.put("role", student.getRole());

        return ResultVO.success(info);
    }

    @GetMapping("/admin/stats")
    public ResultVO<Map<String, Object>> getAdminStats() {
        log.info("获取管理员统计数据");

        long totalUsers = studentMapper.selectCount(null);
        long totalActivities = activityMapper.selectCount(null);
        long totalSignups = signMapper.selectCount(null);
        long totalChecked = signMapper.getCheckedCount();  // 添加这行
        long pendingActivities = signMapper.getPendingCount();
        long ongoingActivities = activityMapper.getOngoingCount();
        long todayActivities = activityMapper.getTodayActivitiesCount();

        // 计算总签到率
        double overallSignRate = totalSignups > 0 ? (totalChecked * 100.0 / totalSignups) : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalActivities", totalActivities);
        stats.put("totalSignups", totalSignups);
        stats.put("overallSignRate", Math.round(overallSignRate * 100) / 100.0);
        stats.put("todayActivities", todayActivities);
        stats.put("pendingActivities", pendingActivities);
        stats.put("ongoingActivities", ongoingActivities);

        return ResultVO.success(stats);
    }

    /**
     * 获取所有用户列表（管理员用）
     */
    @GetMapping("/admin/users")
    public ResultVO<List<Map<String, Object>>> getAllUsers() {
        log.info("获取所有用户列表");
        try {
            List<Map<String, Object>> users = studentMapper.getAllUsersInfo();
            return ResultVO.success("获取成功", users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResultVO.error(500, "获取失败：" + e.getMessage());
        }
    }
}