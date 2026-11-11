package cn.edu.hubu.hubuactivities.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL) // 2. 加在这里：只序列化非 null 的字段
@Data
public class ActivityVO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String typeName;
    private String cover;
    private String startTime;
    private String endTime;
    private String signStart;
    private String signEnd;
    private String location;
    private Integer total;
    private Integer remain;
    private Integer signCount;
    private BigDecimal creditValue;
    private Integer hoursValue;
    private String activityStatus;
    private String statusDisplay;
    private String buttonText;
    private Boolean canSign;
    private String buttonClass;
    private String countdownText;
    private String scoreDisplay;

    // 报名状态（用于我的活动页面）
    private String signStatus;
    private String signTime;
    private String checkinTime;
    private String reviewTime;
    private String reviewRemark;
}