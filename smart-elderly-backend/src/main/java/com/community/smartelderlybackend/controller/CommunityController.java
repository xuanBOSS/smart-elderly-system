package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.BuildingGeoZone;
import com.community.smartelderlybackend.entity.DiagnosisRecord;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.BuildingGeoZoneMapper;
import com.community.smartelderlybackend.mapper.DiagnosisRecordMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private DiagnosisRecordMapper diagnosisRecordMapper;
    @Autowired
    private BuildingGeoZoneMapper buildingGeoZoneMapper;
    @Autowired
    private com.community.smartelderlybackend.service.AiService aiService;

    @GetMapping("/statistics")
    @Operation(summary = "获取社区大屏统计数据")
    public Result<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> data = new HashMap<>();

        // 1. 社区老年人口总数 (role = 0)，精确统计
        Long totalElders = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 0));
        data.put("totalElders", totalElders);

        // 2. 每位老人取「最新一条」健康记录，统计慢病相关人数（不再使用固定 35%）
        List<HealthRecords> allRecordsOrdered = healthRecordsMapper.selectList(new LambdaQueryWrapper<HealthRecords>()
                .orderByDesc(HealthRecords::getRecordTime));
        Map<Long, HealthRecords> latestByElder = new LinkedHashMap<>();
        for (HealthRecords r : allRecordsOrdered) {
            if (r.getUserId() != null && !latestByElder.containsKey(r.getUserId())) {
                latestByElder.put(r.getUserId(), r);
            }
        }
        Map<Long, String> diagnosisByElder = latestDiagnosisByElder();
        int chronicCount = 0;
        for (Map.Entry<Long, HealthRecords> e : latestByElder.entrySet()) {
            String diagnosisType = diagnosisByElder.get(e.getKey());
            if (diagnosisType != null || classifyChronicType(e.getValue()) != null) {
                chronicCount++;
            }
        }
        data.put("chronicElders", chronicCount);

        // 3. 今日门诊预约：仅统计待确认/已确认，排除老人取消与医生拒绝（status=2）
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayAppointments = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .between(Appointment::getAppointTime, startOfToday, endOfToday)
                .in(Appointment::getStatus, 0, 1));
        data.put("todayAppointments", todayAppointments);

        // 4. 本月紧急求助总数，精确统计
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        Long monthlyEmergencies = emergencyRecordMapper.selectCount(new LambdaQueryWrapper<EmergencyRecord>()
                .between(EmergencyRecord::getHelpTime, startOfMonth, endOfMonth));
        data.put("monthlyEmergencies", monthlyEmergencies);

        // 5. 慢病类型分布：按“每位老人仅归入一个主类型”统计，保证柱状图总和与慢病人数一致
        long hypertension = 0, hypotension = 0, diabetes = 0, heartAbnormal = 0;
        for (Map.Entry<Long, HealthRecords> e : latestByElder.entrySet()) {
            String type = diagnosisByElder.get(e.getKey());
            if (type == null) {
                type = classifyChronicType(e.getValue());
            }
            if ("糖尿病".equals(type)) diabetes++;
            else if ("高血压".equals(type)) hypertension++;
            else if ("低血压".equals(type)) hypotension++;
            else if ("心率异常".equals(type)) heartAbnormal++;
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", new String[]{"高血压", "低血压", "糖尿病", "心率异常"});
        chartData.put("values", new Long[]{hypertension, hypotension, diabetes, heartAbnormal});
        data.put("diseaseChart", chartData);

        return Result.success(data);
    }

    @GetMapping("/statDetail")
    @Operation(summary = "统计卡片明细")
    public Result<List<Map<String, Object>>> statDetail(@RequestParam String type) {
        List<Map<String, Object>> rows = new ArrayList<>();
        switch (type) {
            case "elders" -> {
                List<User> elders = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 0)
                        .orderByAsc(User::getUserId)
                        .last("LIMIT 200"));
                for (User u : elders) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", u.getUserId());
                    m.put("name", u.getRealName());
                    m.put("username", u.getUsername());
                    rows.add(m);
                }
            }
            case "chronic" -> {
                List<HealthRecords> allRecordsOrdered = healthRecordsMapper.selectList(new LambdaQueryWrapper<HealthRecords>()
                        .orderByDesc(HealthRecords::getRecordTime));
                Map<Long, HealthRecords> latestByElder = new LinkedHashMap<>();
                Map<Long, String> diagnosisByElder = latestDiagnosisByElder();
                for (HealthRecords r : allRecordsOrdered) {
                    if (r.getUserId() != null && !latestByElder.containsKey(r.getUserId())) {
                        latestByElder.put(r.getUserId(), r);
                    }
                }
                for (Map.Entry<Long, HealthRecords> e : latestByElder.entrySet()) {
                    String chronicType = diagnosisByElder.get(e.getKey());
                    if (chronicType == null) chronicType = classifyChronicType(e.getValue());
                    if (chronicType == null) continue;
                    User u = userMapper.selectById(e.getKey());
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", e.getKey());
                    m.put("name", u != null ? u.getRealName() : "");
                    m.put("diagnosisType", chronicType);
                    rows.add(m);
                }
            }
            case "todayAppointments" -> {
                LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
                List<Appointment> appts = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                        .between(Appointment::getAppointTime, startOfToday, endOfToday)
                        .in(Appointment::getStatus, 0, 1)
                        .orderByAsc(Appointment::getAppointTime));
                DateTimeFormatter tf = DateTimeFormatter.ofPattern("MM-dd HH:mm");
                for (Appointment a : appts) {
                    User elder = userMapper.selectById(a.getUserId());
                    User doctor = userMapper.selectById(a.getDoctorId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("elderName", elder != null ? elder.getRealName() : "未知");
                    m.put("doctorName", doctor != null ? doctor.getRealName() : "未知");
                    m.put("time", a.getAppointTime() != null ? a.getAppointTime().format(tf) : "");
                    m.put("status", a.getStatus() != null && a.getStatus() == 1 ? "已确认" : "待确认");
                    rows.add(m);
                }
            }
            case "monthEmergencies" -> {
                LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
                LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
                List<EmergencyRecord> recs = emergencyRecordMapper.selectList(new LambdaQueryWrapper<EmergencyRecord>()
                        .between(EmergencyRecord::getHelpTime, startOfMonth, endOfMonth)
                        .orderByDesc(EmergencyRecord::getHelpTime)
                        .last("LIMIT 200"));
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm");
                for (EmergencyRecord r : recs) {
                    User elder = userMapper.selectById(r.getUserId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("helpId", r.getHelpId());
                    m.put("time", r.getHelpTime() != null ? r.getHelpTime().format(dtf) : "");
                    m.put("elderName", elder != null ? elder.getRealName() : "未知");
                    m.put("location", r.getLocation());
                    Integer st = r.getStatus();
                    String stText = st == null ? "" : switch (st) {
                        case 0 -> "待处理";
                        case 1 -> "家属处理中";
                        case 2 -> "社区已接单";
                        case 3 -> "已解决";
                        default -> "状态" + st;
                    };
                    m.put("statusText", stText);
                    rows.add(m);
                }
            }
            default -> {
                return Result.error("不支持的明细类型");
            }
        }
        return Result.success(rows);
    }

    /**
     * 读取每位老人的最新有效诊断（active=1）。
     */
    private Map<Long, String> latestDiagnosisByElder() {
        List<DiagnosisRecord> list = diagnosisRecordMapper.selectList(new LambdaQueryWrapper<DiagnosisRecord>()
                .eq(DiagnosisRecord::getActive, 1)
                .orderByDesc(DiagnosisRecord::getDiagnosisTime)
                .orderByDesc(DiagnosisRecord::getDiagnosisId));
        Map<Long, String> map = new LinkedHashMap<>();
        for (DiagnosisRecord d : list) {
            if (d.getUserId() == null || map.containsKey(d.getUserId())) continue;
            String t = normalizeDiagnosisType(d.getDiagnosisType());
            if (t != null) {
                map.put(d.getUserId(), t);
            }
        }
        return map;
    }

    /**
     * 基于健康记录推断“慢病主类型”。
     * 说明：系统没有单独的疾病诊断表，这里使用体征阈值规则进行推断。
     * 为了与慢病人数卡片对齐，每位老人只归入一个类型（按优先级）。
     */
    private String classifyChronicType(HealthRecords r) {
        if (r == null) return null;
        // 优先级：糖尿病 > 高血压 > 低血压 > 心率异常
        if (isDiabetesLatest(r)) return "糖尿病";
        if (isHypertensionLatest(r)) return "高血压";
        if (isHypotensionLatest(r)) return "低血压";
        if (isHeartRateAbnormalLatest(r)) return "心率异常";
        return null;
    }

    private String normalizeDiagnosisType(String type) {
        if (type == null) return null;
        String t = type.trim();
        if (t.isEmpty()) return null;
        if (t.contains("糖")) return "糖尿病";
        if (t.contains("高")) return "高血压";
        if (t.contains("低")) return "低血压";
        if (t.contains("心率")) return "心率异常";
        return null;
    }

    private boolean isHypertensionLatest(HealthRecords r) {
        Float h = r.getBloodPressureHigh();
        Float l = r.getBloodPressureLow();
        return (h != null && h > 140) || (l != null && l >= 90);
    }

    private boolean isHypotensionLatest(HealthRecords r) {
        Float h = r.getBloodPressureHigh();
        Float l = r.getBloodPressureLow();
        return (h != null && h < 90) || (l != null && l < 60);
    }

    private boolean isDiabetesLatest(HealthRecords r) {
        return r.getBloodSugar() != null && r.getBloodSugar() > 7.0;
    }

    private boolean isHeartRateAbnormalLatest(HealthRecords r) {
        Integer hr = r.getHeartRate();
        return hr != null && (hr > 100 || hr < 55);
    }

    @GetMapping("/emergencies")
    @Operation(summary = "获取实时紧急工单列表")
    public Result<List<Map<String, Object>>> getEmergencies() {
        // 展示本月全部工单状态（0待处理、1家属处理中、2社区已接单、3已解决）
        LambdaQueryWrapper<EmergencyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EmergencyRecord::getStatus, 0, 1, 2, 3)
                .orderByDesc(EmergencyRecord::getHelpTime);
        List<EmergencyRecord> records = emergencyRecordMapper.selectList(wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        List<BuildingGeoZone> zones = buildingGeoZoneMapper.selectList(new LambdaQueryWrapper<BuildingGeoZone>()
                .eq(BuildingGeoZone::getActive, 1));
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
            String locationForDisplay = enrichLocationForDisplay(record, zones);
            map.put("content", locationForDisplay + " " + name + " " + (record.getHandleResult() != null ? record.getHandleResult() : "发出求助"));

            // 状态转换
            if (record.getStatus() != null && record.getStatus() == 0) {
                map.put("status", "待处理");
            } else if (record.getStatus() != null && record.getStatus() == 1) {
                map.put("status", "家属处理中");
            } else if (record.getStatus() != null && record.getStatus() == 2) {
                map.put("status", "社区已接单");
            } else if (record.getStatus() != null && record.getStatus() == 3) {
                map.put("status", "已解决");
            } else {
                map.put("status", "未知状态");
            }

            resultList.add(map);
        }
        return Result.success(resultList);
    }

    private static final Pattern BUILDING_PATTERN = Pattern.compile("(\\d{1,2})\\s*(?:栋|号楼)");
    private static final Pattern COORD_PATTERN = Pattern.compile("定位坐标\\(([-\\d.]+),\\s*([-\\d.]+)\\)");

    private String enrichLocationForDisplay(EmergencyRecord record, List<BuildingGeoZone> zones) {
        String raw = record.getLocation() == null ? "" : record.getLocation().trim();
        if (raw.isEmpty()) return "未知地点";

        if (extractBuildingNo(raw).isPresent()) {
            return raw;
        }
        Optional<Integer> byCoord = resolveBuildingByCoordinateText(raw, zones);
        if (byCoord.isPresent()) {
            return "小区" + byCoord.get() + "号楼 " + raw;
        }
        Optional<Integer> byHistory = inferBuildingFromHistory(record.getUserId(), zones);
        if (byHistory.isPresent()) {
            return "小区" + byHistory.get() + "号楼 " + raw;
        }
        return raw;
    }

    private Optional<Integer> extractBuildingNo(String text) {
        if (text == null) return Optional.empty();
        Matcher m = BUILDING_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();
        try {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= 12) return Optional.of(n);
        } catch (Exception ignore) {
            // ignore
        }
        return Optional.empty();
    }

    private Optional<Integer> resolveBuildingByCoordinateText(String text, List<BuildingGeoZone> zones) {
        if (text == null) return Optional.empty();
        Matcher m = COORD_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();
        try {
            double lat = Double.parseDouble(m.group(1));
            double lng = Double.parseDouble(m.group(2));
            return resolveByZones(lat, lng, zones);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private Optional<Integer> inferBuildingFromHistory(Long userId, List<BuildingGeoZone> zones) {
        if (userId == null) return Optional.empty();
        List<EmergencyRecord> history = emergencyRecordMapper.selectList(new LambdaQueryWrapper<EmergencyRecord>()
                .eq(EmergencyRecord::getUserId, userId)
                .isNotNull(EmergencyRecord::getLocation)
                .orderByDesc(EmergencyRecord::getHelpTime)
                .last("LIMIT 20"));
        for (EmergencyRecord r : history) {
            Optional<Integer> direct = extractBuildingNo(r.getLocation());
            if (direct.isPresent()) return direct;
            Optional<Integer> byCoord = resolveBuildingByCoordinateText(r.getLocation(), zones);
            if (byCoord.isPresent()) return byCoord;
        }
        return Optional.empty();
    }

    private Optional<Integer> resolveByZones(double lat, double lng, List<BuildingGeoZone> zones) {
        for (BuildingGeoZone z : zones) {
            if (z.getBuildingNo() == null
                    || z.getMinLatitude() == null || z.getMaxLatitude() == null
                    || z.getMinLongitude() == null || z.getMaxLongitude() == null) {
                continue;
            }
            boolean inLat = lat >= z.getMinLatitude() && lat <= z.getMaxLatitude();
            boolean inLng = lng >= z.getMinLongitude() && lng <= z.getMaxLongitude();
            if (inLat && inLng) return Optional.of(z.getBuildingNo());
        }
        return Optional.empty();
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
