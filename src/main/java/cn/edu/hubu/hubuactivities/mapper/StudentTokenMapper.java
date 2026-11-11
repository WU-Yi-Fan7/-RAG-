package cn.edu.hubu.hubuactivities.mapper;

import cn.edu.hubu.hubuactivities.entity.StudentToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentTokenMapper extends BaseMapper<StudentToken> {

    @Select("SELECT * FROM student_token WHERE token = #{token} AND is_valid = 1 AND token_expire > NOW()")
    StudentToken findByToken(@Param("token") String token);

    @Update("UPDATE student_token SET is_valid = 0 WHERE student_id = #{studentId}")
    void invalidateAllTokens(@Param("studentId") Long studentId);
}