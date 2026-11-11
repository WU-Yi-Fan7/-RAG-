package cn.edu.hubu.hubuactivities.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_type")
public class ActivityType {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("type_name")
    private String typeName;

    @TableField("type_code")
    private String typeCode;

    @TableField("is_credit")
    private Integer isCredit;

    @TableField("is_hours")
    private Integer isHours;

    @TableField("credit_min")
    private Double creditMin;

    @TableField("credit_max")
    private Double creditMax;

    @TableField("hours_min")
    private Integer hoursMin;

    @TableField("hours_max")
    private Integer hoursMax;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;
}