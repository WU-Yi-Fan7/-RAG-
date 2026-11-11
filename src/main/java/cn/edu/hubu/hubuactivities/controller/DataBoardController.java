package cn.edu.hubu.hubuactivities.controller;

import cn.edu.hubu.hubuactivities.mapper.ActivityMapper;
import cn.edu.hubu.hubuactivities.mapper.SignMapper;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DataBoardController {

    private final ActivityMapper activityMapper;
    private final SignMapper signMapper;

    /**
     * 获取数据看板统计
     */
    @GetMapping("/databoard")
    public ResultVO<Map<String, Object>> getDataBoard() {
        log.info("获取数据看板统计");

        Map<String, Object> result = new HashMap<>();

        // 各活动报名趋势
        List<Map<String, Object>> activityTrend = activityMapper.getActivitySignTrend();
        result.put("activityTrend", activityTrend);

        // 各类型活动统计
        List<Map<String, Object>> categoryStats = activityMapper.getCategoryStats();
        result.put("categoryStats", categoryStats);

        // 总体统计（所有活动）
        long totalActivities = activityMapper.selectCount(null);
        long totalSignups = signMapper.selectCount(null);
        long totalChecked = signMapper.getCheckedCount();

        result.put("totalActivities", totalActivities);
        result.put("totalSignups", totalSignups);
        result.put("totalChecked", totalChecked);

        double overallRate = totalSignups > 0 ? (totalChecked * 100.0 / totalSignups) : 0;
        result.put("overallSignRate", Math.round(overallRate * 100) / 100.0);

        return ResultVO.success(result);
    }
}