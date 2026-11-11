package cn.edu.hubu.hubuactivities.mapper;

import cn.edu.hubu.hubuactivities.entity.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    @Select("SELECT * FROM student WHERE stu_id = #{stuId}")
    Student findByStuId(@Param("stuId") String stuId);

    @Select("SELECT * FROM student WHERE phone = #{phone}")
    Student findByPhone(@Param("phone") String phone);
    // 增加诚信分
    @Update("UPDATE student SET credit = LEAST(credit + 1, 100) WHERE id = #{studentId}")
    int incrementCredit(@Param("studentId") Long studentId);

    // 减少诚信分
    @Update("UPDATE student SET credit = credit - 1 WHERE id = #{studentId} AND credit > 0")
    int decrementCredit(@Param("studentId") Long studentId);
    // 获取所有用户信息（只返回需要的字段）
    @Select("SELECT id, stu_id, real_name, phone, school_name, department, major, grade, political_status, credit, role, status FROM student ORDER BY id")
    List<Map<String, Object>> getAllUsersInfo();
}