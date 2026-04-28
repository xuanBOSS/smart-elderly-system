package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.DiagnosisRecord;
import com.community.smartelderlybackend.entity.FamilyBind;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.DiagnosisRecordMapper;
import com.community.smartelderlybackend.mapper.FamilyBindMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health")
@Tag(name = "健康大盘模块")
public class HealthController {

    @Autowired
    private HealthRecordsMapper healthRecordsMapper;
    @Autowired
    private FamilyBindMapper familyBindMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private DiagnosisRecordMapper diagnosisRecordMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/dashboard")
    @Operation(summary = "获取指定老人的健康大盘")
    public Result<Map<String, Object>> getDashboard(@RequestParam Long elderId, HttpServletRequest request) {
        Integer role = request.getAttribute("role") instanceof Number n ? n.intValue() : null;
        Long tokenUserId = request.getAttribute("userId") instanceof Number n ? n.longValue() : null;
        if (role != null && role == 1 && tokenUserId != null) {
            Long bound = familyBindMapper.selectCount(new LambdaQueryWrapper<FamilyBind>()
                    .eq(FamilyBind::getFamilyId, tokenUserId)
                    .eq(FamilyBind::getElderId, elderId));
            if (bound == null || bound == 0) {
                return Result.error("您未绑定该老人或无权查看");
            }
        } else if (role != null && role == 2 && tokenUserId != null) {
            Long hasPatient = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getDoctorId, tokenUserId)
                    .eq(Appointment::getUserId, elderId)
                    .eq(Appointment::getStatus, 1));
            if (hasPatient == null || hasPatient == 0) {
                return Result.error("仅可查看已确认预约关联的患者健康数据");
            }
        }

        LambdaQueryWrapper<HealthRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecords::getUserId, elderId)
                .orderByDesc(HealthRecords::getRecordTime)
                .orderByDesc(HealthRecords::getRecordId)
                .last("LIMIT 7");
        List<HealthRecords> records = healthRecordsMapper.selectList(wrapper);

        Map<String, Object> finalData = buildEmptyDashboard();
        if (records == null || records.isEmpty()) {
            return Result.success(finalData);
        }

        Collections.reverse(records);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", records.stream().map(r -> r.getRecordTime().format(formatter)).collect(Collectors.toList()));
        trend.put("systolic", records.stream().map(HealthRecords::getBloodPressureHigh).collect(Collectors.toList()));
        trend.put("diastolic", records.stream().map(HealthRecords::getBloodPressureLow).collect(Collectors.toList()));
        trend.put("heartRate", records.stream().map(HealthRecords::getHeartRate).collect(Collectors.toList()));

        HealthRecords latestRecord = records.get(records.size() - 1);
        Map<String, Object> latest = new HashMap<>();
        String bp = (latestRecord.getBloodPressureHigh() != null ? latestRecord.getBloodPressureHigh().toString() : "--")
                + "/"
                + (latestRecord.getBloodPressureLow() != null ? latestRecord.getBloodPressureLow().toString() : "--");
        latest.put("bloodPressure", bp);
        latest.put("heartRate", latestRecord.getHeartRate() != null ? latestRecord.getHeartRate().toString() : "--");
        latest.put("bloodSugar", latestRecord.getBloodSugar() != null ? latestRecord.getBloodSugar().toString() : "--");
        String medicationInfo = latestRecord.getMedicationInfo();
        if (medicationInfo == null || medicationInfo.trim().isEmpty()) {
            // 若最新记录未填写用药要求，回退到最近一条有用药信息的记录
            HealthRecords latestMedicationRecord = healthRecordsMapper.selectOne(new LambdaQueryWrapper<HealthRecords>()
                    .eq(HealthRecords::getUserId, elderId)
                    .isNotNull(HealthRecords::getMedicationInfo)
                    .ne(HealthRecords::getMedicationInfo, "")
                    .orderByDesc(HealthRecords::getRecordTime)
                    .orderByDesc(HealthRecords::getRecordId)
                    .last("LIMIT 1"));
            medicationInfo = latestMedicationRecord != null ? latestMedicationRecord.getMedicationInfo() : "";
        }
        latest.put("medicationInfo", sanitizeMedicationInfo(medicationInfo));

        finalData.put("trend", trend);
        finalData.put("latest", latest);
        finalData.put("diagnosisRecords", listDiagnosisRecords(elderId));
        return Result.success(finalData);
    }

    private List<Map<String, Object>> listDiagnosisRecords(Long elderId) {
        List<DiagnosisRecord> records = diagnosisRecordMapper.selectList(new LambdaQueryWrapper<DiagnosisRecord>()
                .eq(DiagnosisRecord::getUserId, elderId)
                .eq(DiagnosisRecord::getActive, 1)
                .orderByDesc(DiagnosisRecord::getDiagnosisTime)
                .last("LIMIT 20"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DiagnosisRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("diagnosisId", r.getDiagnosisId());
            m.put("type", r.getDiagnosisType() == null ? "未标注" : r.getDiagnosisType());
            m.put("note", r.getNote() == null ? "" : r.getNote());
            m.put("time", r.getDiagnosisTime() == null ? "--" : r.getDiagnosisTime().format(dtf));
            String doctorName = "系统记录";
            if (r.getDoctorId() != null) {
                User doctor = userMapper.selectById(r.getDoctorId());
                if (doctor != null && doctor.getRealName() != null && !doctor.getRealName().isBlank()) {
                    doctorName = doctor.getRealName();
                }
            }
            m.put("doctorName", doctorName);
            rows.add(m);
        }
        return rows;
    }

    private Map<String, Object> buildEmptyDashboard() {
        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", new ArrayList<>());
        trend.put("systolic", new ArrayList<>());
        trend.put("diastolic", new ArrayList<>());
        trend.put("heartRate", new ArrayList<>());

        Map<String, Object> latest = new HashMap<>();
        latest.put("bloodPressure", "--/--");
        latest.put("heartRate", "--");
        latest.put("bloodSugar", "--");
        latest.put("medicationInfo", "");

        Map<String, Object> data = new HashMap<>();
        data.put("trend", trend);
        data.put("latest", latest);
        data.put("diagnosisRecords", new ArrayList<>());
        return data;
    }

    /**
     * 用药展示清洗：保留“药名+用法”，去掉“指标评价/复查结论”等非用药描述。
     * 例如：阿司匹林，每日一次，血压极佳 -> 阿司匹林，每日一次
     */
    private String sanitizeMedicationInfo(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        String[] pieces = s.split("[,，;；。]");
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < pieces.length; i++) {
            String p = pieces[i] == null ? "" : pieces[i].trim();
            if (p.isEmpty()) continue;
            if (i == 0) {
                kept.add(p);
                continue;
            }
            if (containsMedicationRuleKeyword(p)) {
                kept.add(p);
            }
        }
        if (kept.isEmpty()) return s;
        return String.join("，", kept);
    }

    private boolean containsMedicationRuleKeyword(String text) {
        return text.contains("每日")
                || text.contains("每晚")
                || text.contains("每周")
                || text.contains("每次")
                || text.contains("次")
                || text.contains("服")
                || text.contains("注射")
                || text.contains("餐前")
                || text.contains("餐后")
                || text.contains("必要时")
                || text.contains("qd")
                || text.contains("bid")
                || text.contains("tid");
    }
}
