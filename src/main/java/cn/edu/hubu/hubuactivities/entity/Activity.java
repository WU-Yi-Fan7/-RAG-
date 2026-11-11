package cn.edu.hubu.hubuactivities.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("category")
    private String category;

    @TableField("type_id")
    private Integer typeId;

    @TableField("type_code")
    private String typeCode;

    // 添加这个字段用于接收联表查询的类型名称
    @TableField(exist = false)  // 表示这个字段在数据库表中不存在
    private String typeName;

    @TableField("cover")
    private String cover;

    @TableField("start_time")
    private LocalDateTime start_time;

    @TableField("end_time")
    private LocalDateTime end_time;

    @TableField("sign_start")
    private LocalDateTime sign_start;

    @TableField("sign_end")
    private LocalDateTime sign_end;

    @TableField("location")
    private String location;

    @TableField("total")
    private Integer total;

    @TableField("remain")
    private Integer remain;

    @TableField("need_review")
    private Integer need_review;

    @TableField("days_before")
    private Integer days_before;

    @TableField("days_min")
    private Integer days_min;

    @TableField("days_max")
    private Integer days_max;

    @TableField("qr_enabled")
    private Integer qr_enabled;

    @TableField("num_enabled")
    private Integer num_enabled;

    @TableField("num_code")
    private String num_code;

    @TableField("num_gen_time")
    private LocalDateTime num_gen_time;

    @TableField("num_expire")
    private LocalDateTime num_expire;

    @TableField("credit_absent")
    private Integer credit_absent;

    @TableField("credit_sign")
    private Integer credit_sign;

    @TableField("credit_min")
    private Integer credit_min;

    @TableField("credit_value")
    private BigDecimal credit_value;

    @TableField("max_credit")
    private BigDecimal max_credit;

    @TableField("hours_value")
    private Integer hours_value;

    @TableField("max_hours")
    private Integer max_hours;

    @TableField("view_count")
    private Integer view_count;

    @TableField("sign_count")
    private Integer sign_count;

    @TableField("sign_ok")
    private Integer sign_ok;

    @TableField("status")
    private String status;

    @TableField("activity_status")
    private String activity_status;

    @TableField("publisher_id")
    private Long publisher_id;

    @TableField("publish_time")
    private LocalDateTime publish_time;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime create_time;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime update_time;
    // 报名状态（用于我的活动页面）
    @TableField(exist = false)
    private String signStatus;

    // 报名时间
    @TableField(exist = false)
    private LocalDateTime signTime;

    // 签到时间
    @TableField(exist = false)
    private LocalDateTime checkinTime;

    // 审核时间
    @TableField(exist = false)
    private LocalDateTime reviewTime;

    // 审核备注
    @TableField(exist = false)
    private String reviewRemark;
    // 签到二维码/数字码
    @TableField(exist = false)
    private String checkinCode;

    // 签到码类型（qr:二维码, num:数字码）
    @TableField(exist = false)
    private String codeType;
    // 签到二维码（下划线命名）
    @TableField("qr_code")
    private String qr_code;

    // 保留原来的 getter/setter
    public String getQr_code() {
        return qr_code;
    }

    public void setQr_code(String qr_code) {
        this.qr_code = qr_code;
    }
    // 报名状态（用于我的活动页面，来自联表查询）
    @TableField(exist = false)
    private String sign_status;

    @TableField(exist = false)
    private LocalDateTime sign_time;

    @TableField(exist = false)
    private LocalDateTime checkin_time;

    @TableField(exist = false)
    private LocalDateTime review_time;

    @TableField(exist = false)
    private String review_remark;

}