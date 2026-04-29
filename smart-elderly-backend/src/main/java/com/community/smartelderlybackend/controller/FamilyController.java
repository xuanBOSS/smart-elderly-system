package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.dto.ElderlyAppointmentRequest;
import com.community.smartelderlybackend.entity.FamilyBind;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.DoctorScheduleMapper;
import com.community.smartelderlybackend.mapper.EmergencyRecordMapper;
import com.community.smartelderlybackend.mapper.FamilyBindMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @Autowired
    private EmergencyRecordMapper emergencyRecordMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;
    @Autowired
    private com.community.smartelderlybackend.service.ElderlyAppointmentService elderlyAppointmentService;

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
    public Result<Map<String, Object>> getDashboard(@RequestParam Long elderId, HttpServletRequest request) {
        if (!isFamilyBoundToElder(request, elderId)) {
            return Result.error("您未绑定该老人或无权查看");
        }
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        trend.put("dates", records.stream().map(r -> r.getRecordTime().format(formatter)).collect(Collectors.toList()));
        trend.put("systolic", records.stream().map(HealthRecords::getBloodPressureHigh).collect(Collectors.toList()));
        trend.put("diastolic", records.stream().map(HealthRecords::getBloodPressureLow).collect(Collectors.toList()));
        trend.put("heartRate", records.stream().map(HealthRecords::getHeartRate).collect(Collectors.toList()));

        // 4. 组装最新指标 Latest 数据
        HealthRecords latestRecord = records.get(records.size() - 1);
        Map<String, Object> latest = new HashMap<>();
        latest.put("bloodPressure", latestRecord.getBloodPressureHigh() + "/" + latestRecord.getBloodPressureLow());
        latest.put("heartRate", latestRecord.getHeartRate() != null ? latestRecord.getHeartRate().toString() : "--");
        latest.put("bloodSugar", latestRecord.getBloodSugar() != null ? latestRecord.getBloodSugar().toString() : "--");
        latest.put("medicationInfo", latestRecord.getMedicationInfo() != null ? latestRecord.getMedicationInfo() : "");

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

    @GetMapping("/doctors-for-booking")
    @Operation(summary = "家属为绑定老人预约时可选的医生列表")
    public Result<List<User>> doctorsForBooking(@RequestParam Long elderId, HttpServletRequest request) {
        if (!isFamilyBoundToElder(request, elderId)) {
            return Result.error("您未绑定该老人或无权操作");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, 2);
        return Result.success(userMapper.selectList(wrapper));
    }

    @GetMapping("/doctorSchedules")
    @Operation(summary = "家属端：某医生从今日起的排班（用于选日期）")
    public Result<List<Map<String, Object>>> familyDoctorSchedules(
            @RequestParam Long elderId,
            @RequestParam Long doctorId,
            HttpServletRequest request) {
        if (!isFamilyBoundToElder(request, elderId)) {
            return Result.error("您未绑定该老人或无权操作");
        }
        User doctor = userMapper.selectById(doctorId);
        if (doctor == null || doctor.getRole() == null || doctor.getRole() != 2) {
            return Result.error("医生不存在");
        }
        return Result.success(elderlyAppointmentService.listSchedulesForDoctor(doctorId, LocalDate.now()));
    }

    @PostMapping("/appointment")
    @Operation(summary = "家属代为提交老人预约挂号")
    public Result<Map<String, Object>> bookAppointmentForElder(
            @RequestBody ElderlyAppointmentRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || request.getUserId() == null) {
            return Result.error("userId（老人ID）不能为空");
        }
        if (!isFamilyBoundToElder(httpRequest, request.getUserId())) {
            return Result.error("您未绑定该老人或无权代为预约");
        }
        return elderlyAppointmentService.createPendingAppointment(request);
    }

    @GetMapping("/appointments")
    @Operation(summary = "家属查看绑定老人的预约记录")
    public Result<List<Map<String, Object>>> getElderAppointments(
            @RequestParam Long elderId,
            HttpServletRequest request) {
        if (!isFamilyBoundToElder(request, elderId)) {
            return Result.error("您未绑定该老人或无权查看");
        }
        List<Appointment> appointments = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, elderId)
                .orderByDesc(Appointment::getAppointTime));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Appointment appt : appointments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", appt.getAppointId());
            map.put("appointId", appt.getAppointId());
            map.put("doctorId", appt.getDoctorId());
            User doctor = userMapper.selectById(appt.getDoctorId());
            map.put("doctorName", doctor != null ? doctor.getRealName() : "未知医生");
            map.put("createTime", appt.getAppointTime() != null ? appt.getAppointTime().format(formatter) : "");
            Integer status = appt.getStatus();
            String statusText = "未知";
            if (status != null) {
                if (status == 0) statusText = "待确认";
                else if (status == 1) statusText = "已确认";
                else if (status == 2) statusText = "已取消";
            }
            map.put("status", statusText);
            resultList.add(map);
        }
        return Result.success(resultList);
    }

    @PostMapping("/appointment/cancel")
    @Operation(summary = "家属取消绑定老人的预约")
    public Result<String> cancelElderAppointment(
            @RequestParam Long elderId,
            @RequestParam Long appointId,
            HttpServletRequest request) {
        if (elderId == null || appointId == null) {
            return Result.error("elderId/appointId 不能为空");
        }
        if (!isFamilyBoundToElder(request, elderId)) {
            return Result.error("您未绑定该老人或无权操作");
        }

        Appointment appointment = appointmentMapper.selectById(appointId);
        if (appointment == null) {
            return Result.error("未找到该预约记录");
        }
        if (appointment.getUserId() == null || !appointment.getUserId().equals(elderId)) {
            return Result.error("不能取消不属于该老人的预约");
        }

        Integer oldStatus = appointment.getStatus();
        if (oldStatus != null && oldStatus == 2) {
            return Result.success("预约已取消");
        }

        // 如果原本是已确认预约，需要回退排班已约人数
        if (oldStatus != null && oldStatus == 1 && appointment.getAppointTime() != null && appointment.getDoctorId() != null) {
            LocalDate appointDate = appointment.getAppointTime().toLocalDate();
            int hour = appointment.getAppointTime().getHour();
            int timeSlot = hour < 12 ? 0 : 1; // 0上午, 1下午

            DoctorSchedule schedule = doctorScheduleMapper.selectOne(new LambdaQueryWrapper<DoctorSchedule>()
                    .eq(DoctorSchedule::getDoctorId, appointment.getDoctorId())
                    .eq(DoctorSchedule::getWorkDate, appointDate)
                    .eq(DoctorSchedule::getTimeSlot, timeSlot));

            if (schedule != null) {
                int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
                schedule.setBookedCount(Math.max(bookedCount - 1, 0));
                doctorScheduleMapper.updateById(schedule);
            }
        }

        appointment.setStatus(2);
        appointmentMapper.updateById(appointment);
        return Result.success("预约已取消");
    }

    @PostMapping("/emergency/resolve")
    @Operation(summary = "家属处理完成后回传工单已解决")
    public Result<String> resolveEmergencyByFamily(
            @RequestParam Long helpId,
            HttpServletRequest request) {
        if (helpId == null) {
            return Result.error("helpId 不能为空");
        }
        Long familyId = Long.valueOf(request.getAttribute("userId").toString());
        EmergencyRecord record = emergencyRecordMapper.selectById(helpId);
        if (record == null) {
            return Result.error("工单不存在");
        }
        if (record.getUserId() == null) {
            return Result.error("工单缺少老人信息");
        }
        Long bindCount = familyBindMapper.selectCount(new LambdaQueryWrapper<FamilyBind>()
                .eq(FamilyBind::getFamilyId, familyId)
                .eq(FamilyBind::getElderId, record.getUserId()));
        if (bindCount == null || bindCount <= 0) {
            return Result.error("您无权处理该老人的工单");
        }
        Integer st = record.getStatus();
        if (st != null && st == 3) {
            return Result.success("工单已是已解决状态");
        }
        // 仅允许家属处理中(status=1)或待处理(status=0)直接完结
        if (st != null && st != 0 && st != 1) {
            return Result.error("当前状态不可由家属直接完结");
        }
        String old = record.getHandleResult() == null ? "" : record.getHandleResult().trim();
        String tag = "(家属已解决)";
        if (!old.contains(tag)) {
            old = old.isEmpty() ? tag : (old + " " + tag);
        }
        record.setStatus(3);
        record.setHandleResult(old);
        emergencyRecordMapper.updateById(record);
        return Result.success("已回传社区：家属处理完成");
    }

    private boolean isFamilyBoundToElder(HttpServletRequest request, Long elderId) {
        if (elderId == null) {
            return false;
        }
        Long familyId = Long.valueOf(request.getAttribute("userId").toString());
        Long count = familyBindMapper.selectCount(new LambdaQueryWrapper<FamilyBind>()
                .eq(FamilyBind::getFamilyId, familyId)
                .eq(FamilyBind::getElderId, elderId));
        return count != null && count > 0;
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