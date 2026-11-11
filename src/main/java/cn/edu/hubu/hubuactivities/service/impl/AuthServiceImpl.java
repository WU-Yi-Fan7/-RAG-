package cn.edu.hubu.hubuactivities.service.impl;

import cn.edu.hubu.hubuactivities.config.BusinessException;
import cn.edu.hubu.hubuactivities.entity.Student;
import cn.edu.hubu.hubuactivities.entity.StudentToken;
import cn.edu.hubu.hubuactivities.mapper.StudentMapper;
import cn.edu.hubu.hubuactivities.mapper.StudentTokenMapper;
import cn.edu.hubu.hubuactivities.service.AuthService;
import cn.edu.hubu.hubuactivities.utils.JwtUtils;
import cn.edu.hubu.hubuactivities.utils.PasswordUtils;
import cn.edu.hubu.hubuactivities.vo.LoginVO;
import cn.edu.hubu.hubuactivities.vo.RegisterVO;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@RestControllerAdvice // 核心注解：开启全局异常拦截
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;
    private final StudentTokenMapper tokenMapper;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<?> login(LoginVO loginVO, HttpServletRequest request) {
            log.info("===== 登录请求开始 =====");
            log.info("学号: {}, 密码: {}", loginVO.getStu_id(), loginVO.getPassword());

            // 1. 查询学生
            Student student = studentMapper.findByStuId(loginVO.getStu_id());

            if (student == null) {
                log.warn("用户不存在");
                throw new BusinessException(401, "学号或密码错误");
            }

            log.info("数据库中的密码: [{}]", student.getPassword());

            // 2. 直接比较明文密码
            if (!loginVO.getPassword().equals(student.getPassword())) {
                log.warn("密码验证失败");
                return ResultVO.error(401, "学号或密码错误");
            }

            log.info("密码验证成功");

            // 3. 检查账号状态
            if (student.getIs_black() == 1) {
                log.warn("账号已被拉黑");
                return ResultVO.error(403, "账号已被加入黑名单");
            }

            // 4. 生成JWT令牌
            String token = jwtUtils.generateToken(student);
            String refreshToken = jwtUtils.generateRefreshToken(student);

            // 5. 使旧token失效
            tokenMapper.invalidateAllTokens(student.getId());

            // 6. 保存新token
            StudentToken studentToken = new StudentToken();
            studentToken.setStudent_id(student.getId());
            studentToken.setStu_id(student.getStu_id());
            studentToken.setToken(token);
            studentToken.setToken_type("Bearer");
            studentToken.setToken_expire(LocalDateTime.now().plusHours(2));
            studentToken.setRefresh_token(refreshToken);
            studentToken.setRefresh_expire(LocalDateTime.now().plusDays(7));
            studentToken.setLogin_time(LocalDateTime.now());
            studentToken.setLogin_ip(getIpAddress(request));
            studentToken.setUser_agent(request.getHeader("User-Agent"));
            studentToken.setIs_valid(1);

            tokenMapper.insert(studentToken);

            // 7. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("refreshToken", refreshToken);
            data.put("tokenType", "Bearer");

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", student.getId());
            userInfo.put("stu_id", student.getStu_id());
            userInfo.put("real_name", student.getReal_name());
            userInfo.put("role", student.getRole());
            userInfo.put("credit", student.getCredit());
            userInfo.put("school_name", student.getSchool_name());
            userInfo.put("department", student.getDepartment());
            userInfo.put("major", student.getMajor());
            userInfo.put("phone", student.getPhone());

            data.put("userInfo", userInfo);

            log.info("登录成功：{}", student.getStu_id());
            return ResultVO.success("登录成功", data);

    }

    @Override
    @Transactional
    public ResultVO<?> register(RegisterVO registerVO) {
        try {
            log.info("===== 注册请求开始 =====");
            log.info("学号: {}, 姓名: {}, 手机号: {}",
                    registerVO.getStu_id(), registerVO.getReal_name(), registerVO.getPhone());

            // 1. 检查学号是否已存在
            Student existingStudent = studentMapper.findByStuId(registerVO.getStu_id());
            if (existingStudent != null) {
                log.warn("学号已存在: {}", registerVO.getStu_id());
                return ResultVO.error(409, "该学号已注册");
            }

            // 2. 检查手机号是否已存在
            Student existingPhone = studentMapper.findByPhone(registerVO.getPhone());
            if (existingPhone != null) {
                log.warn("手机号已存在: {}", registerVO.getPhone());
                return ResultVO.error(409, "该手机号已注册");
            }

            // 3. 创建新学生
            Student student = new Student();
            student.setStu_id(registerVO.getStu_id());
            student.setPassword(registerVO.getPassword());  // 直接存明文
            student.setSalt("no-salt");  // 占位
            student.setReal_name(registerVO.getReal_name());
            student.setPhone(registerVO.getPhone());
            student.setSchool_name(registerVO.getSchool_name());
            student.setDepartment(registerVO.getDepartment());
            student.setMajor(registerVO.getMajor());

            // 从学号提取年级（前4位）
            String grade = registerVO.getStu_id().substring(0, 4);
            student.setGrade(grade);

            // 政治面貌，默认"群众"
            if (registerVO.getPolitical_status() != null && !registerVO.getPolitical_status().isEmpty()) {
                student.setPolitical_status(registerVO.getPolitical_status());
            } else {
                student.setPolitical_status("群众");
            }

            // 设置默认值
            student.setCredit(100);  // 新用户默认100分
            student.setSigned_count(0);
            student.setAbsent_count(0);
            student.setIs_black(0);
            student.setRole("user");  // 默认普通用户
            student.setStatus(1);      // 正常状态

            // 4. 保存到数据库
            int result = studentMapper.insert(student);

            if (result > 0) {
                log.info("注册成功，学号: {}, 姓名: {}", student.getStu_id(), student.getReal_name());

                // 5. 构建返回数据
                Map<String, Object> data = new HashMap<>();
                data.put("id", student.getId());
                data.put("stu_id", student.getStu_id());
                data.put("real_name", student.getReal_name());
                data.put("credit", student.getCredit());

                return ResultVO.success("注册成功", data);
            } else {
                log.error("注册失败，数据库插入失败");
                return ResultVO.error(500, "注册失败，请重试");
            }

        } catch (Exception e) {
            log.error("注册异常", e);
            return ResultVO.error(500, "注册失败：" + e.getMessage());
        }
    }

    @Override
    public ResultVO<?> refreshToken(String refreshToken) {
        try {
            // 1. 验证刷新token是否有效
            if (!jwtUtils.validateToken(refreshToken)) {
                log.warn("刷新token无效或已过期");
                return ResultVO.error(401, "刷新令牌无效或已过期");
            }

            // 2. 从刷新token中获取学号
            String stuId = jwtUtils.getStuIdFromToken(refreshToken);

            // 3. 查询用户
            Student student = studentMapper.findByStuId(stuId);
            if (student == null) {
                log.warn("刷新token失败：用户不存在 - 学号: {}", stuId);
                return ResultVO.error(401, "用户不存在");
            }

            // 4. 生成新token
            String newToken = jwtUtils.generateToken(student);
            String newRefreshToken = jwtUtils.generateRefreshToken(student);

            // 5. 保存新token
            StudentToken studentToken = new StudentToken();
            studentToken.setStudent_id(student.getId());
            studentToken.setStu_id(student.getStu_id());
            studentToken.setToken(newToken);
            studentToken.setToken_type("Bearer");
            studentToken.setToken_expire(LocalDateTime.now().plusHours(2));
            studentToken.setRefresh_token(newRefreshToken);
            studentToken.setRefresh_expire(LocalDateTime.now().plusDays(7));
            studentToken.setLogin_time(LocalDateTime.now());
            studentToken.setIs_valid(1);

            tokenMapper.insert(studentToken);

            // 6. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("refreshToken", newRefreshToken);
            data.put("tokenType", "Bearer");
            data.put("expiresIn", 7200);

            log.info("token刷新成功：学号: {}", stuId);
            return ResultVO.success("刷新成功", data);

        } catch (Exception e) {
            log.error("刷新token异常: {}", e.getMessage(), e);
            return ResultVO.error(500, "刷新失败");
        }
    }

    @Override
    @Transactional
    public ResultVO<?> logout(String token) {
        try {
            log.info("开始处理退出登录");

            // 1. 查询token记录
            StudentToken studentToken = tokenMapper.findByToken(token);

            if (studentToken != null) {
                // 2. 使token失效
                studentToken.setIs_valid(0);
                studentToken.setLogout_time(LocalDateTime.now());
                tokenMapper.updateById(studentToken);
                log.info("token已失效，学号: {}", studentToken.getStu_id());
            }

            return ResultVO.success("退出成功", null);

        } catch (Exception e) {
            log.error("退出登录异常: {}", e.getMessage(), e);
            return ResultVO.error(500, "退出失败");
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}