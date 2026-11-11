package cn.edu.hubu.hubuactivities.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("stu_id")  // 数据库字段名
    private String stu_id;  // 字段名必须和数据库一致

    @TableField("password")
    private String password;

    @TableField("salt")
    private String salt;

    @TableField("real_name")
    private String real_name;

    @TableField("phone")
    private String phone;

    @TableField("school_name")
    private String school_name;

    @TableField("department")
    private String department;

    @TableField("major")
    private String major;

    @TableField("grade")
    private String grade;

    @TableField("political_status")
    private String political_status;

    @TableField("credit")
    private Integer credit;

    @TableField("signed_count")
    private Integer signed_count;

    @TableField("absent_count")
    private Integer absent_count;

    @TableField("is_black")
    private Integer is_black;

    @TableField("black_reason")
    private String black_reason;

    @TableField("black_time")
    private LocalDateTime black_time;

    @TableField("role")
    private String role;

    @TableField("status")
    private Integer status;

    @TableField("last_login_time")
    private LocalDateTime last_login_time;

    @TableField("last_login_ip")
    private String last_login_ip;

    @TableField("last_login_device")
    private String last_login_device;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime create_time;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime update_time;
}