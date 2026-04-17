package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/dashboard")
    @Operation(summary = "获取指定老人的健康大盘")
    public Result<Map<String, Object>> getDashboard(@RequestParam Long elderId) {
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", records.stream().map(r -> r.getRecordTime().format(formatter)).collect(Collectors.toList()));
        trend.put("systolic", records.stream().map(HealthRecords::getBloodPressureHigh).collect(Collectors.toList()));
        trend.put("diastolic", records.stream().map(HealthRecords::getBloodPressureLow).collect(Collectors.toList()));
        trend.put("heartRate", records.stream().map(HealthRecords::getHeartRate).collect(Collectors.toList()));

        HealthRecords latestRecord = records.get(records.size() - 1);
        Map<String, Object> latest = new HashMap<>();
        latest.put("bloodPressure", latestRecord.getBloodPressureHigh() + "/" + latestRecord.getBloodPressureLow());
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
