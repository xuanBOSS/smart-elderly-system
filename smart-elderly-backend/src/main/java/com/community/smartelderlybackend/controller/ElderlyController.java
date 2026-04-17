package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.dto.ElderlyAppointmentRequest;
import com.community.smartelderlybackend.dto.EmergencyRequest;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.DoctorScheduleMapper;
import com.community.smartelderlybackend.mapper.EmergencyRecordMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elderly")
@Tag(name = "老年人端模块")
public class ElderlyController {

    @Autowired
    private EmergencyRecordMapper emergencyRecordMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;
    @Autowired
    private HealthRecordsMapper healthRecordsMapper;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/emergency")
    @Operation(summary = "一键报警接口")
    public Result<Map<String, Object>> createEmergency(@RequestBody EmergencyRequest request) {
        if (request == null || request.getUserId() == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(request.getUserId());
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        String location = request.getLocation();
        if (location == null || location.trim().isEmpty()) {
            Double latitude = request.getLatitude();
            Double longitude = request.getLongitude();

            if (latitude != null && longitude != null) {
                // 位置先只显示经纬度，不做反向地理编码，避免外部服务失败导致“未登记家庭住址”
                location = buildCoordinateText(latitude, longitude);
            }

            // 如果仍然为空，再退回“最近一次登记的位置”
            if (location == null || location.trim().isEmpty()) {
                EmergencyRecord latestRecord = emergencyRecordMapper.selectOne(new LambdaQueryWrapper<EmergencyRecord>()
                        .eq(EmergencyRecord::getUserId, request.getUserId())
                        .isNotNull(EmergencyRecord::getLocation)
                        .orderByDesc(EmergencyRecord::getHelpTime)
                        .last("LIMIT 1"));
                location = latestRecord != null ? latestRecord.getLocation() : "未登记家庭住址";
            }
        }

        EmergencyRecord record = new EmergencyRecord();
        record.setUserId(request.getUserId());
        record.setHelpTime(LocalDateTime.now());
        record.setLocation(location);
        record.setStatus(0);
        record.setHandleResult("老人发起一键报警");
        emergencyRecordMapper.insert(record);

        Map<String, Object> data = new HashMap<>();
        data.put("helpId", record.getHelpId());
        data.put("userId", record.getUserId());
        data.put("location", record.getLocation());
        data.put("handleResult", record.getHandleResult());
        data.put("status", record.getStatus());
        return Result.success(data);
    }

    private String buildCoordinateText(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return "";
        return String.format("定位坐标(%.6f, %.6f)", latitude, longitude);
    }

    @GetMapping("/doctors")
    @Operation(summary = "获取所有医生列表（role=2）")
    public Result<List<User>> getDoctors(@RequestParam Long userId) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(userId);
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, 2);
        List<User> doctors = userMapper.selectList(wrapper);
        return Result.success(doctors);
    }

    @PostMapping("/appointment")
    @Operation(summary = "挂号预约接口")
    public Result<Map<String, Object>> handleAppointment(@RequestBody(required = false) ElderlyAppointmentRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("schedules", queryTodaySchedules());

        if (request == null || request.getDoctorId() == null) {
            return Result.success(data);
        }
        if (request.getUserId() == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(request.getUserId());
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        DoctorSchedule schedule = request.getScheduleId() != null
                ? doctorScheduleMapper.selectById(request.getScheduleId())
                : doctorScheduleMapper.selectOne(new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDoctorId, request.getDoctorId())
                .eq(DoctorSchedule::getWorkDate, LocalDate.now())
                .orderByAsc(DoctorSchedule::getTimeSlot)
                .last("LIMIT 1"));

        if (schedule == null) {
            // 容错：如果当天没有排班，找从今天起最近的一条排班
            schedule = doctorScheduleMapper.selectOne(new LambdaQueryWrapper<DoctorSchedule>()
                    .eq(DoctorSchedule::getDoctorId, request.getDoctorId())
                    .ge(DoctorSchedule::getWorkDate, LocalDate.now())
                    .orderByAsc(DoctorSchedule::getWorkDate)
                    .orderByAsc(DoctorSchedule::getTimeSlot)
                    .last("LIMIT 1"));
            if (schedule == null) {
                return Result.error("未找到可用排班");
            }
        }

        int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
        int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();
        // 待确认预约(status=0)也占用号源，因此需要与已约(bookedCount)一起计算剩余名额
        int timeSlot = schedule.getTimeSlot() == null ? 0 : schedule.getTimeSlot(); // 0上午, 1下午
        LocalDate workDate = schedule.getWorkDate();
        LocalDateTime start = workDate.atStartOfDay();
        LocalDateTime end = workDate.atTime(LocalTime.MAX);

        List<Appointment> pendingAppts = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, schedule.getDoctorId())
                .eq(Appointment::getStatus, 0)
                .between(Appointment::getAppointTime, start, end));

        long pendingCount = pendingAppts.stream()
                .filter(a -> a.getAppointTime() != null)
                .filter(a -> {
                    int hour = a.getAppointTime().getHour();
                    int apptSlot = hour < 12 ? 0 : 1;
                    return apptSlot == timeSlot;
                })
                .count();

        int reservedCount = bookedCount + (int) pendingCount;
        if (maxCapacity > 0 && reservedCount >= maxCapacity) {
            return Result.error("该排班号源已满");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(request.getUserId());
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setStatus(0);
        appointment.setAppointTime(resolveAppointTime(schedule, request.getAppointTime()));
        appointmentMapper.insert(appointment);

        Map<String, Object> result = new HashMap<>(data);
        result.put("appointId", appointment.getAppointId());
        result.put("status", appointment.getStatus());
        result.put("appointTime", appointment.getAppointTime());
        return Result.success(result);
    }

    @GetMapping("/appointments")
    @Operation(summary = "获取当前老人的预约记录")
    public Result<List<Map<String, Object>>> getMyAppointments(@RequestParam Long userId) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(userId);
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getUserId, userId)
                .orderByDesc(Appointment::getAppointTime);

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");

        List<Map<String, Object>> resultList = new java.util.ArrayList<>();
        for (Appointment appt : appointments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", appt.getAppointId());
            map.put("appointId", appt.getAppointId());
            map.put("doctorId", appt.getDoctorId());

            User doctor = userMapper.selectById(appt.getDoctorId());
            map.put("doctorName", doctor != null ? doctor.getRealName() : "未知医生");
            map.put("department", "老年医学门诊");
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
    @Operation(summary = "取消预约接口")
    public Result<String> cancelAppointment(@RequestParam Long appointId, @RequestParam Long userId) {
        if (appointId == null || userId == null) {
            return Result.error("appointId/userId 不能为空");
        }

        User elder = userMapper.selectById(userId);
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        Appointment appointment = appointmentMapper.selectById(appointId);
        if (appointment == null) {
            return Result.error("未找到该预约记录");
        }
        if (appointment.getUserId() == null || !appointment.getUserId().equals(userId)) {
            return Result.error("不能取消他人的预约");
        }

        Integer oldStatus = appointment.getStatus();
        if (oldStatus != null && oldStatus == 2) {
            return Result.success("预约已取消");
        }

        // 如果原本是已确认预约，需要回退排班已约人数
        if (oldStatus != null && oldStatus == 1 && appointment.getAppointTime() != null && appointment.getDoctorId() != null) {
            java.time.LocalDate appointDate = appointment.getAppointTime().toLocalDate();
            int hour = appointment.getAppointTime().getHour();
            int timeSlot = hour < 12 ? 0 : 1; // 0上午, 1下午（与 DoctorController 保持一致）

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

        appointment.setStatus(2); // 0待处理, 1已确认, 2已取消/拒绝
        appointmentMapper.updateById(appointment);
        return Result.success("预约已取消");
    }

    @GetMapping("/health")
    @Operation(summary = "健康档案拉取接口")
    public Result<Map<String, Object>> getHealth(@RequestParam Long userId) {
        if (userId == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(userId);
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }

        HealthRecords latestRecord = healthRecordsMapper.selectOne(new LambdaQueryWrapper<HealthRecords>()
                .eq(HealthRecords::getUserId, userId)
                .orderByDesc(HealthRecords::getRecordTime)
                .last("LIMIT 1"));

        if (latestRecord == null) {
            return Result.error("暂无健康档案");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", latestRecord.getRecordId());
        data.put("userId", latestRecord.getUserId());
        data.put("bloodPressureHigh", latestRecord.getBloodPressureHigh());
        data.put("bloodPressureLow", latestRecord.getBloodPressureLow());
        data.put("heartRate", latestRecord.getHeartRate());
        data.put("bloodSugar", latestRecord.getBloodSugar());
        data.put("medicationInfo", latestRecord.getMedicationInfo());
        data.put("recordTime", latestRecord.getRecordTime());
        return Result.success(data);
    }

    private List<Map<String, Object>> queryTodaySchedules() {
        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getWorkDate, LocalDate.now())
                .orderByAsc(DoctorSchedule::getTimeSlot));

        // 一次性拉取今天所有待确认预约(status=0)，用于计算“已约(bookedCount) + 待确认占用”的剩余号源
        List<Long> doctorIds = schedules.stream().map(DoctorSchedule::getDoctorId).distinct().toList();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        List<Appointment> pendingAppts = doctorIds.isEmpty()
                ? java.util.Collections.emptyList()
                : appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .in(Appointment::getDoctorId, doctorIds)
                .eq(Appointment::getStatus, 0)
                .between(Appointment::getAppointTime, start, end));

        return schedules.stream().map(schedule -> {
            Map<String, Object> map = new HashMap<>();
            User doctor = userMapper.selectById(schedule.getDoctorId());

            int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
            int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();

            int timeSlot = schedule.getTimeSlot() == null ? 0 : schedule.getTimeSlot(); // 0上午, 1下午
            long pendingCount = pendingAppts.stream()
                    .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(schedule.getDoctorId()))
                    .filter(a -> a.getAppointTime() != null)
                    .filter(a -> {
                        int hour = a.getAppointTime().getHour();
                        int apptSlot = hour < 12 ? 0 : 1;
                        return apptSlot == timeSlot;
                    })
                    .count();

            int reservedCount = bookedCount + (int) pendingCount;

            map.put("scheduleId", schedule.getScheduleId());
            map.put("doctorId", schedule.getDoctorId());
            map.put("doctorName", doctor != null ? doctor.getRealName() : "未知医生");
            map.put("workDate", schedule.getWorkDate());
            map.put("timeSlot", schedule.getTimeSlot());
            map.put("timeSlotText", schedule.getTimeSlot() != null && schedule.getTimeSlot() == 0 ? "上午" : "下午");
            map.put("maxCapacity", maxCapacity);
            map.put("bookedCount", bookedCount);
            map.put("remainCount", Math.max(maxCapacity - reservedCount, 0));
            return map;
        }).toList();
    }

    private LocalDateTime resolveAppointTime(DoctorSchedule schedule, LocalDateTime appointTime) {
        if (appointTime != null) {
            return appointTime;
        }
        LocalTime time = schedule.getTimeSlot() != null && schedule.getTimeSlot() == 1
                ? LocalTime.of(14, 30)
                : LocalTime.of(9, 0);
        return LocalDateTime.of(schedule.getWorkDate(), time);
    }

    private boolean isElderlyUser(User user) {
        return user.getRole() != null && user.getRole() == 0;
    }
}
