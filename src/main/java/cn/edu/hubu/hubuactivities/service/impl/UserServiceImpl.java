package cn.edu.hubu.hubuactivities.service.impl;

import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.entity.Sign;
import cn.edu.hubu.hubuactivities.entity.Student;
import cn.edu.hubu.hubuactivities.mapper.ActivityMapper;
import cn.edu.hubu.hubuactivities.mapper.SignMapper;
import cn.edu.hubu.hubuactivities.mapper.StudentMapper;
import cn.edu.hubu.hubuactivities.mapper.StudentTokenMapper;
import cn.edu.hubu.hubuactivities.service.UserService;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import cn.edu.hubu.hubuactivities.vo.UserStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StudentMapper studentMapper;
    private final SignMapper signMapper;
    private final ActivityMapper activityMapper;
    private final StudentTokenMapper tokenMapper;

    @Override
    public ResultVO<UserStatsVO> getUserStats(String stuId) {
        try {
            log.info("获取用户统计数据，学号: {}", stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            UserStatsVO stats = new UserStatsVO();
            stats.setRealName(student.getReal_name());
            stats.setCredit(student.getCredit());

            // 统计总积分和总工时（从已签到的活动中累计）
            LambdaQueryWrapper<Sign> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Sign::getStudentId, student.getId())
                    .eq(Sign::getStatus, "checked");
            List<Sign> signList = signMapper.selectList(wrapper);

            BigDecimal totalPoints = BigDecimal.ZERO;
            Integer totalHours = 0;

            for (Sign sign : signList) {
                Activity activity = activityMapper.selectById(sign.getActivityId());
                if (activity != null) {
                    if (activity.getCredit_value() != null) {
                        totalPoints = totalPoints.add(activity.getCredit_value());
                    }
                    if (activity.getHours_value() != null) {
                        totalHours += activity.getHours_value();
                    }
                }
            }

            stats.setTotalPoints(totalPoints);
            stats.setTotalHours(totalHours);

            return ResultVO.success("获取成功", stats);
        } catch (Exception e) {
            log.error("获取用户统计失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    public ResultVO<Student> getStudentInfo(String stuId) {
        try {
            log.info("获取学生详细信息，学号: {}", stuId);

            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            // 返回需要的信息（密码等敏感信息不返回）
            Student result = new Student();
            result.setReal_name(student.getReal_name());
            result.setSchool_name(student.getSchool_name());
            result.setDepartment(student.getDepartment());
            result.setMajor(student.getMajor());
            result.setGrade(student.getGrade());
            result.setStu_id(student.getStu_id());
            result.setPolitical_status(student.getPolitical_status());

            return ResultVO.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return ResultVO.error(500, "获取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<?> changePassword(String stuId, String oldPassword, String newPassword) {
        try {
            log.info("修改密码，学号: {}", stuId);

            // 1. 查询用户
            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                return ResultVO.error(404, "用户不存在");
            }

            // 2. 验证原密码（明文比较）
            if (!oldPassword.equals(student.getPassword())) {
                return ResultVO.error(400, "原密码错误");
            }

            // 3. 验证新密码长度
            if (newPassword == null || newPassword.length() < 6) {
                return ResultVO.error(400, "新密码长度不能少于6位");
            }

            // 4. 更新密码
            student.setPassword(newPassword);
            int result = studentMapper.updateById(student);

            if (result > 0) {
                // 5. 使该用户所有token失效
                tokenMapper.invalidateAllTokens(student.getId());
                log.info("密码修改成功，学号: {}", stuId);
                return ResultVO.success("密码修改成功", null);
            } else {
                return ResultVO.error(500, "密码修改失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResultVO.error(500, "修改密码失败：" + e.getMessage());
        }
    }
}