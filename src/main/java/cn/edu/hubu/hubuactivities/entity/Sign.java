package cn.edu.hubu.hubuactivities.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_sign")
public class Sign {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("student_id")
    private Long studentId;

    @TableField("sign_time")
    private LocalDateTime signTime;

    @TableField("checkin_time")
    private LocalDateTime checkinTime;

    @TableField("status")
    private String status;  // pending待审核, approved已录取, rejected未录取, checked已签到, cancelled已取消

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_remark")
    private String reviewRemark;

    // 签到码
    @TableField(exist = false)
    private String checkinCode;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}