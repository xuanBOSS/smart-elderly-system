package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.FamilyBind;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.FamilyBindMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
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

        finalData.put("trend", trend);
        finalData.put("latest", latest);
        return Result.success(finalData);
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

        Map<String, Object> data = new HashMap<>();
        data.put("trend", trend);
        data.put("latest", latest);
        return data;
    }
}
