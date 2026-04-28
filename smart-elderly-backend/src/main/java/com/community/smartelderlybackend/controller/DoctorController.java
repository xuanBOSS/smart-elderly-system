package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.DiagnosisRecord;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.DiagnosisRecordMapper;
import com.community.smartelderlybackend.mapper.DoctorScheduleMapper;
import com.community.smartelderlybackend.mapper.HealthRecordsMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/doctor")
@Tag(name = "医生端模块")
public class DoctorController {

    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private HealthRecordsMapper healthRecordsMapper;
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;
    @Autowired
    private DiagnosisRecordMapper diagnosisRecordMapper;

    @GetMapping("/appointments/pending")
    @Operation(summary = "获取当前医生的待处理预约列表")
    public Result<List<Map<String, Object>>> getPendingAppointments(HttpServletRequest request) {
        // 1. 从 Token 中安全获取当前登录医生的 ID
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());

        // 2. 查询该医生所有“待处理(status=0)”的预约记录，按时间排序
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getStatus, 0)
                .orderByAsc(Appointment::getAppointTime);
        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // 3. 组装前端需要的 Table 格式数据
        List<Map<String, Object>> resultList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd a hh:mm");

        for (Appointment appt : appointments) {
            Map<String, Object> map = new HashMap<>();
            map.put("appointId", appt.getAppointId()); // 必须传给前端，后续用来点击确认/拒绝

            // 查出患者名字
            User patient = userMapper.selectById(appt.getUserId());
            map.put("name", patient != null ? patient.getRealName() : "未知患者");

            // 格式化时间，例如：12/22 上午09:00
            map.put("time", appt.getAppointTime().format(formatter).replace("AM", "上午").replace("PM", "下午"));
            map.put("type", "门诊"); // 目前咱们库里暂未区分门诊/上门，写死即可
            map.put("status", "待确认");

            resultList.add(map);
        }

        return Result.success(resultList);
    }

    @PostMapping("/appointments/handle")
    @Operation(summary = "处理预约(确认或拒绝)")
    public Result<String> handleAppointment(
            @RequestParam Long appointId,
            @RequestParam Integer action,
            HttpServletRequest request) {
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());

        Appointment appointment = appointmentMapper.selectById(appointId);
        if (appointment == null) {
            return Result.error("未找到该预约记录");
        }
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorId)) {
            return Result.error("无权处理他人的预约");
        }
        Integer oldStatus = appointment.getStatus();

        // 1. 更新预约单状态
        appointment.setStatus(action);
        appointmentMapper.updateById(appointment);

        // 2. 若由“待确认(0)”改为“已确认(1)”，同步增加排班 booked_count
        if ((oldStatus == null || Integer.valueOf(0).equals(oldStatus)) && Integer.valueOf(1).equals(action)
                && appointment.getAppointTime() != null && appointment.getDoctorId() != null) {
            LocalDate workDate = appointment.getAppointTime().toLocalDate();
            int timeSlot = appointment.getAppointTime().getHour() < 12 ? 0 : 1;
            DoctorSchedule schedule = doctorScheduleMapper.selectOne(new LambdaQueryWrapper<DoctorSchedule>()
                    .eq(DoctorSchedule::getDoctorId, appointment.getDoctorId())
                    .eq(DoctorSchedule::getWorkDate, workDate)
                    .eq(DoctorSchedule::getTimeSlot, timeSlot));
            if (schedule != null) {
                int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
                int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();
                int nextBooked = maxCapacity > 0 ? Math.min(bookedCount + 1, maxCapacity) : bookedCount + 1;
                schedule.setBookedCount(nextBooked);
                doctorScheduleMapper.updateById(schedule);
            }
        }

        return Result.success(action == 1 ? "预约已确认" : "预约已拒绝");
    }

    @GetMapping("/patients")
    @Operation(summary = "获取当前医生的患者档案列表(已确认预约的患者)")
    public Result<List<Map<String, Object>>> getPatientArchives(HttpServletRequest request) {
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());

        // 1. 查出所有指派给该医生且已确认(status=1)的预约
        LambdaQueryWrapper<Appointment> apptWrapper = new LambdaQueryWrapper<>();
        apptWrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getStatus, 1);
        List<Appointment> confirmedAppts = appointmentMapper.selectList(apptWrapper);

        // 2. 提取不重复的老人ID (使用 Java 8 Stream 优雅去重)
        List<Long> elderIds = confirmedAppts.stream()
                .map(Appointment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (elderIds.isEmpty()) {
            return Result.success(resultList);
        }

        // 3. 遍历这些老人，组装他们的档案和最新健康状况
        for (Long elderId : elderIds) {
            User elder = userMapper.selectById(elderId);
            if (elder == null) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("elderId", elder.getUserId());
            map.put("name", elder.getRealName());
            map.put("age", elder.getAge());

            // 查该老人最新的一条体检记录
            LambdaQueryWrapper<HealthRecords> hrWrapper = new LambdaQueryWrapper<>();
            hrWrapper.eq(HealthRecords::getUserId, elderId)
                    .orderByDesc(HealthRecords::getRecordTime)
                    .last("LIMIT 1");
            HealthRecords latestRecord = healthRecordsMapper.selectOne(hrWrapper);

            if (latestRecord != null) {
                map.put("lastVisit", latestRecord.getRecordTime().format(DateTimeFormatter.ofPattern("MM/dd")));

                // 简单的 AI 疾病推断逻辑（后续可以换成真的大模型预测）
                String condition = "指标平稳";
                if (latestRecord.getBloodPressureHigh() >= 140) condition = "血压偏高";
                else if (latestRecord.getBloodSugar() >= 7.0) condition = "血糖偏高";
                else if (latestRecord.getHeartRate() > 100) condition = "心率过快";

                map.put("condition", condition);
            } else {
                map.put("lastVisit", "暂无记录");
                map.put("condition", "缺乏数据");
            }
            resultList.add(map);
        }

        return Result.success(resultList);
    }

    @GetMapping("/schedule")
    @Operation(summary = "获取当前医生的排班计划")
    public Result<List<Map<String, Object>>> getMySchedule(HttpServletRequest request) {
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());

        // 1. 查询该医生的所有排班，按日期先后顺序排列
        LambdaQueryWrapper<DoctorSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DoctorSchedule::getDoctorId, doctorId)
                .orderByAsc(DoctorSchedule::getWorkDate);
        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(wrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd");
        String[] weekDays = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        // 2. 遍历组装前端需要的格式
        for (DoctorSchedule schedule : schedules) {
            Map<String, Object> map = new HashMap<>();

            // 算出这天是星期几
            int dayOfWeek = schedule.getWorkDate().getDayOfWeek().getValue();
            map.put("day", weekDays[dayOfWeek]);

            // 组装具体时间和日期
            String timeStr = schedule.getTimeSlot() == 0 ? "上午 08:00 - 12:00" : "下午 14:00 - 18:00";
            map.put("time", schedule.getWorkDate().format(dateFormatter) + " " + timeStr);

            // 组装备注信息（统一从 appointment 实时统计）
            int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();

            java.time.LocalDate workDate = schedule.getWorkDate();
            LocalDateTime start = workDate.atStartOfDay();
            LocalDateTime end = workDate.atTime(LocalTime.MAX);

            List<Appointment> activeAppts = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getDoctorId, doctorId)
                    .in(Appointment::getStatus, 0, 1)
                    .between(Appointment::getAppointTime, start, end));

            int timeSlot = schedule.getTimeSlot() == null ? 0 : schedule.getTimeSlot();
            long pendingCount = activeAppts.stream()
                    .filter(a -> a.getAppointTime() != null)
                    .filter(a -> a.getStatus() != null && a.getStatus() == 0)
                    .filter(a -> {
                        int hour = a.getAppointTime().getHour();
                        int apptSlot = hour < 12 ? 0 : 1;
                        return apptSlot == timeSlot;
                    })
                    .count();
            long confirmedCount = activeAppts.stream()
                    .filter(a -> a.getAppointTime() != null)
                    .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                    .filter(a -> {
                        int hour = a.getAppointTime().getHour();
                        int apptSlot = hour < 12 ? 0 : 1;
                        return apptSlot == timeSlot;
                    })
                    .count();

            // 用于判断是否满号：已约(确认) + 待确认(占用但未接诊)
            int reservedCount = (int) confirmedCount + (int) pendingCount;
            String fullTag = (maxCapacity > 0 && reservedCount >= maxCapacity) ? " 满" : "";

            // 关键：这里把“已约”和“待确认”拆开显示
            // 这样老人提交会增加待确认；医生确认后待确认减少、已约增加，数字会明显变化。
            map.put("note",
                    "门诊接诊 (已约: " + confirmedCount + " / " + maxCapacity +
                            "，待确认: " + pendingCount + fullTag + ")");
            map.put("workDate", schedule.getWorkDate().toString());
            map.put("scheduleId", schedule.getScheduleId());
            map.put("timeSlot", schedule.getTimeSlot());

            resultList.add(map);
        }

        return Result.success(resultList);
    }

    @GetMapping("/schedule/dayPatients")
    @Operation(summary = "按排班日期查看已预约患者")
    public Result<List<Map<String, Object>>> getDayPatients(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String workDate,
            @RequestParam(required = false) Integer timeSlot,
            HttpServletRequest request) {
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());
        LocalDate date;
        Integer slot;

        if (scheduleId != null) {
            DoctorSchedule schedule = doctorScheduleMapper.selectById(scheduleId);
            if (schedule == null || schedule.getDoctorId() == null || !schedule.getDoctorId().equals(doctorId)) {
                return Result.error("排班不存在或无权限查看");
            }
            date = schedule.getWorkDate();
            slot = schedule.getTimeSlot();
        } else if (workDate != null && !workDate.isBlank() && timeSlot != null) {
            date = LocalDate.parse(workDate);
            slot = timeSlot;
        } else {
            return Result.error("请提供 scheduleId 或 workDate+timeSlot");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Appointment> appointments = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorId)
                .in(Appointment::getStatus, 0, 1)
                .between(Appointment::getAppointTime, start, end)
                .orderByAsc(Appointment::getAppointTime));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Appointment appt : appointments) {
            if (appt.getAppointTime() == null) continue;
            int apptSlot = appt.getAppointTime().getHour() < 12 ? 0 : 1;
            if (!apptSlotEquals(slot, apptSlot)) continue;

            User elder = userMapper.selectById(appt.getUserId());
            Map<String, Object> map = new HashMap<>();
            map.put("elderName", elder != null ? elder.getRealName() : "未知患者");
            map.put("appointTime", appt.getAppointTime() != null ? appt.getAppointTime().format(formatter) : "");
            map.put("statusText", appt.getStatus() != null && appt.getStatus() == 1 ? "已确认" : "待确认");
            rows.add(map);
        }
        return Result.success(rows);
    }

    private boolean apptSlotEquals(Integer scheduleSlot, int apptSlot) {
        return scheduleSlot != null && scheduleSlot == apptSlot;
    }

    @PostMapping("/diagnosis")
    @Operation(summary = "医生新增患者诊断记录")
    public Result<String> createDiagnosis(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Long doctorId = Long.valueOf(request.getAttribute("userId").toString());
        Long elderId = payload.get("elderId") == null ? null : Long.valueOf(payload.get("elderId").toString());
        String diagnosisType = payload.get("diagnosisType") == null ? "" : payload.get("diagnosisType").toString().trim();
        String note = payload.get("note") == null ? "" : payload.get("note").toString().trim();

        if (elderId == null) {
            return Result.error("elderId 不能为空");
        }
        if (diagnosisType.isEmpty()) {
            return Result.error("诊断类型不能为空");
        }
        List<String> supported = List.of("高血压", "低血压", "糖尿病", "心率异常");
        if (!supported.contains(diagnosisType)) {
            return Result.error("诊断类型不支持");
        }

        Long hasPatient = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getUserId, elderId)
                .eq(Appointment::getStatus, 1));
        if (hasPatient == null || hasPatient == 0) {
            return Result.error("仅可为已就诊患者新增诊断");
        }

        DiagnosisRecord record = new DiagnosisRecord();
        record.setUserId(elderId);
        record.setDoctorId(doctorId);
        record.setDiagnosisType(diagnosisType);
        record.setNote(note);
        record.setActive(1);
        record.setDiagnosisTime(LocalDateTime.now());
        diagnosisRecordMapper.insert(record);
        return Result.success("诊断记录已保存");
    }
}