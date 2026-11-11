package cn.edu.hubu.hubuactivities.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserStatsVO {
    private String realName;
    private BigDecimal totalPoints;
    private Integer totalHours;
    private Integer credit;
}