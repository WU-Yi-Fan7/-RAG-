package cn.edu.hubu.hubuactivities.service;

import cn.edu.hubu.hubuactivities.entity.Student;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import cn.edu.hubu.hubuactivities.vo.UserStatsVO;

public interface UserService {

    ResultVO<UserStatsVO> getUserStats(String stuId);

    ResultVO<Student> getStudentInfo(String stuId);

    ResultVO<?> changePassword(String stuId, String oldPassword, String newPassword);
}