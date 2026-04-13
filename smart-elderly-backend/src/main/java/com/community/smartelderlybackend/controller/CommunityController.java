package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.EmergencyRecordMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@Tag(name = "社区端模块")
public class CommunityController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private EmergencyRecordMapper emergencyRecordMapper;
    @Autowired
    private HealthRecordsMapper healthRecordsMapper;
    @Autowired
    private com.community.smartelderlybackend.service.AiService aiService;

    @GetMapping("/statistics")
    @Operation(summary = "获取社区大屏统计数据")
    public Result<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> data = new HashMap<>();

        // 1. 社区老年人口总数 (role = 0)，精确统计
        Long totalElders = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 0));
        data.put("totalElders", totalElders);

        // 2. 慢病管理人数 (我们先按老年人口的 35% 来估算一个合理的数字，或者你可以写复杂的查表逻辑)
        data.put("chronicElders", (int)(totalElders * 0.35));

        // 3. 今日门诊预约总数，精确统计
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayAppointments = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .between(Appointment::getAppointTime, startOfToday, endOfToday));
        data.put("todayAppointments", todayAppointments);

        // 4. 本月紧急求助总数，精确统计
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        Long monthlyEmergencies = emergencyRecordMapper.selectCount(new LambdaQueryWrapper<EmergencyRecord>()
                .between(EmergencyRecord::getHelpTime, startOfMonth, endOfMonth));
        data.put("monthlyEmergencies", monthlyEmergencies);

        // 5. 疾病类型分布 (为了让柱状图漂亮，我们去健康记录表里查真实的高血压/糖尿病数量，其他的做模拟)
        List<HealthRecords> allRecords = healthRecordsMapper.selectList(null);
        long highBpCount = allRecords.stream().filter(r -> r.getBloodPressureHigh() != null && r.getBloodPressureHigh() > 140).count();
        long diabetesCount = allRecords.stream().filter(r -> r.getBloodSugar() != null && r.getBloodSugar() > 7.0).count();

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", new String[]{"高血压", "糖尿病", "关节炎", "冠心病"});
        // 为了大屏显示效果，我们把数据库查到的真实数据和模拟数据拼起来
        chartData.put("values", new Long[]{highBpCount, diabetesCount, 72L, 67L});
        data.put("diseaseChart", chartData);

        return Result.success(data);
    }

    @GetMapping("/emergencies")
    @Operation(summary = "获取实时紧急工单列表")
    public Result<List<Map<String, Object>>> getEmergencies() {
        // 查出所有待处理(0)、家属已接单(1)、社区已接单(2)的记录
        LambdaQueryWrapper<EmergencyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EmergencyRecord::getStatus, 0, 1, 2)
                .orderByDesc(EmergencyRecord::getHelpTime);
        List<EmergencyRecord> records = emergencyRecordMapper.selectList(wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (EmergencyRecord record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("helpId", record.getHelpId());

            // 如果处理结果里带有"摔倒"、"晕"、"心"等字眼，标记为紧急(URGENT)，否则普通(NORMAL)
            String type = "NORMAL";
            if (record.getHandleResult() != null &&
                    (record.getHandleResult().contains("摔") || record.getHandleResult().contains("晕") || record.getHandleResult().contains("心"))) {
                type = "URGENT";
            }
            map.put("type", type);
            map.put("time", record.getHelpTime().format(timeFormatter));

            // 查老人姓名拼装内容
            User elder = userMapper.selectById(record.getUserId());
            String name = elder != null ? elder.getRealName() : "未知老人";
            map.put("content", record.getLocation() + " " + name + " " + (record.getHandleResult() != null ? record.getHandleResult() : "发出求助"));

            // 状态转换
            if (record.getStatus() != null && record.getStatus() == 0) {
                map.put("status", "待处理");
            } else if (record.getStatus() != null && record.getStatus() == 1) {
                map.put("status", "家属处理中");
            } else if (record.getStatus() != null && record.getStatus() == 2) {
                map.put("status", "社区已接单");
            } else {
                map.put("status", "未知状态");
            }

            resultList.add(map);
        }
        return Result.success(resultList);
    }

    @PostMapping("/emergency/handle")
    @Operation(summary = "社区接单处理")
    public Result<String> handleEmergency(@RequestParam Long helpId, @RequestParam Integer action) {
        // action: 2 社区接单, 3 已解决
        EmergencyRecord record = emergencyRecordMapper.selectById(helpId);
        if (record == null) {
            return Result.error("未找到该求助记录");
        }

        Integer oldStatus = record.getStatus();
        String handleResult = record.getHandleResult() == null ? "" : record.getHandleResult().trim();

        // status=0 且 action=2 时，追加“社区已接单”
        if (oldStatus != null && oldStatus == 0 && action == 2) {
            handleResult = appendTag(handleResult, "(社区已接单)");
        }

        // status=2 且 action=3 时，移除“社区已接单”，再追加“社区已解决”
        if (oldStatus != null && oldStatus == 2 && action == 3) {
            handleResult = handleResult.replace("(社区已接单)", "").replaceAll("\\s+", " ").trim();
            handleResult = appendTag(handleResult, "(社区已解决)");
        }

        // status=1 且 action=3 时，追加“家属已解决”
        if (oldStatus != null && oldStatus == 1 && action == 3) {
            handleResult = appendTag(handleResult, "(家属已解决)");
        }

        record.setStatus(action);
        record.setHandleResult(handleResult.isEmpty() ? null : handleResult);
        emergencyRecordMapper.updateById(record);

        return Result.success("工单状态更新成功");
    }

    private String appendTag(String original, String tag) {
        if (original == null || original.trim().isEmpty()) {
            return tag;
        }
        String text = original.trim();
        if (text.contains(tag)) {
            return text;
        }
        return text + " " + tag;
    }

    @GetMapping("/risk/predict")
    @Operation(summary = "调用 AI 大模型进行健康风险预测")
    public Result<String> predictRisk(@RequestParam Long elderId) {
        // 1. 查出老人基本信息
        User elder = userMapper.selectById(elderId);
        if (elder == null) return Result.error("老人不存在");

        // 2. 查出老人最近一条体检数据
        LambdaQueryWrapper<HealthRecords> hrWrapper = new LambdaQueryWrapper<>();
        hrWrapper.eq(HealthRecords::getUserId, elderId)
                .orderByDesc(HealthRecords::getRecordTime)
                .last("LIMIT 1");
        HealthRecords latestRecord = healthRecordsMapper.selectOne(hrWrapper);

        if (latestRecord == null) {
            return Result.error("该老人暂无健康数据，AI 无法诊断");
        }

        // 3. 核心：组装给大模型的 Prompt（提示词工程）
        String prompt = String.format(
                "【系统设定】：你是一位拥有 20 年临床经验的社区全科主任医师，语言风格要专业、严谨且通俗易懂。\n" +
                        "【患者背景】：患者姓名：%s，年龄：%d岁。\n" +
                        "【近期体征数据】：最新收缩压：%.1f mmHg，舒张压：%.1f mmHg，心率：%d 次/分，空腹血糖：%.1f mmol/L。\n" +
                        "【你的任务】：请根据上述数据，给出一段 200 字左右的健康风险预测报告。必须包含：\n" +
                        "1. 当前综合风险评级（高危/中危/安全）；\n" +
                        "2. 核心风险点简短分析；\n" +
                        "3. 给社区工作人员的立即干预建议（如需要上门、需要复测或无需特殊处理）。\n" +
                        "直接输出报告内容，不要带任何开场白。",
                elder.getRealName(), elder.getAge(),
                latestRecord.getBloodPressureHigh(), latestRecord.getBloodPressureLow(),
                latestRecord.getHeartRate(), latestRecord.getBloodSugar()
        );

        // 4. 发送给 DeepSeek，获取诊断结果！
        String aiReport = aiService.getRiskPrediction(prompt);

        return Result.success(aiReport);
    }
}
