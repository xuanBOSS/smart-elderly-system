package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.FamilyBind;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.FamilyBindMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/family")
@Tag(name = "家属端模块")
public class FamilyController {

    @Autowired
    private FamilyBindMapper familyBindMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private HealthRecordsMapper healthRecordsMapper;

    @GetMapping("/elders")
    @Operation(summary = "获取当前家属绑定的老人列表")
    public Result<List<Map<String, Object>>> getBindElders(HttpServletRequest request) {
        // 1. 从 JWT 拦截器里获取当前登录的家属 ID！(架构师 A 同学的杰作)
        Long familyId = Long.valueOf(request.getAttribute("userId").toString());

        // 2. 查绑定表
        LambdaQueryWrapper<FamilyBind> bindWrapper = new LambdaQueryWrapper<>();
        bindWrapper.eq(FamilyBind::getFamilyId, familyId);
        List<FamilyBind> binds = familyBindMapper.selectList(bindWrapper);

        // 3. 查出老人的名字拼装返回
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (FamilyBind bind : binds) {
            User elder = userMapper.selectById(bind.getElderId());
            if (elder != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("elderId", elder.getUserId());
                map.put("name", elder.getRealName());
                resultList.add(map);
            }
        }
        return Result.success(resultList);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "获取老人的健康图表大盘")
    public Result<Map<String, Object>> getDashboard(@RequestParam Long elderId) {
        // 1. 查最近 7 条健康记录
        LambdaQueryWrapper<HealthRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecords::getUserId, elderId)
                .orderByDesc(HealthRecords::getRecordTime)
                .last("LIMIT 7");
        List<HealthRecords> records = healthRecordsMapper.selectList(wrapper);

        Map<String, Object> finalData = buildEmptyDashboard();
        if (records == null || records.isEmpty()) {
            return Result.success(finalData);
        }

        // 2. 翻转顺序（让时间从左到右）
        Collections.reverse(records);

        // 3. 组装图表 Trend 数据 (Java 8 Stream )
        Map<String, Object> trend = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        trend.put("dates", records.stream().map(r -> r.getRecordTime().format(formatter)).collect(Collectors.toList()));
        trend.put("systolic", records.stream().map(HealthRecords::getBloodPressureHigh).collect(Collectors.toList()));
        trend.put("diastolic", records.stream().map(HealthRecords::getBloodPressureLow).collect(Collectors.toList()));
        trend.put("heartRate", records.stream().map(HealthRecords::getHeartRate).collect(Collectors.toList()));

        // 4. 组装最新指标 Latest 数据
        HealthRecords latestRecord = records.get(records.size() - 1);
        Map<String, Object> latest = new HashMap<>();
        latest.put("bloodPressure", latestRecord.getBloodPressureHigh() + "/" + latestRecord.getBloodPressureLow());
        latest.put("heartRate", latestRecord.getHeartRate().toString());
        latest.put("bloodSugar", latestRecord.getBloodSugar() != null ? latestRecord.getBloodSugar().toString() : "--");

        finalData.put("trend", trend);
        finalData.put("latest", latest);

        return Result.success(finalData);
    }

    //新增：绑定老人功能
    @PostMapping("/bind")
    @Operation(summary = "绑定新老人")
    public Result<String> bindElder(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long familyId = Long.valueOf(request.getAttribute("userId").toString());
        String elderUsername = params.get("username");
        String relation = params.get("relation");

        // 1. 校验老人账号是否存在且角色为老人(role=0)
        User elder = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, elderUsername)
                .eq(User::getRole, 0));
        if (elder == null) {
            return Result.error("未找到该老人账号，请检查输入");
        }

        // 2. 校验是否已经绑定过该老人
        Long count = familyBindMapper.selectCount(new LambdaQueryWrapper<FamilyBind>()
                .eq(FamilyBind::getFamilyId, familyId)
                .eq(FamilyBind::getElderId, elder.getUserId()));
        if (count > 0) {
            return Result.error("您已绑定过该老人，请勿重复操作");
        }

        // 3. 执行绑定插入
        FamilyBind bind = new FamilyBind();
        bind.setFamilyId(familyId);
        bind.setElderId(elder.getUserId());
        bind.setRelation(relation);
        familyBindMapper.insert(bind);

        return Result.success("绑定成功");
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