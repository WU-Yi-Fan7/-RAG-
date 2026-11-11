package cn.edu.hubu.hubuactivities.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_token")
public class StudentToken {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long student_id;

    @TableField("stu_id")
    private String stu_id;

    @TableField("token")
    private String token;

    @TableField("token_type")
    private String token_type;

    @TableField("token_expire")
    private LocalDateTime token_expire;

    @TableField("refresh_token")
    private String refresh_token;

    @TableField("refresh_expire")
    private LocalDateTime refresh_expire;

    @TableField("login_time")
    private LocalDateTime login_time;

    @TableField("login_ip")
    private String login_ip;

    @TableField("device_info")
    private String device_info;

    @TableField("user_agent")
    private String user_agent;

    @TableField("is_valid")
    private Integer is_valid;

    @TableField("logout_time")
    private LocalDateTime logout_time;
}