package com.community.smartelderlybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.dto.ElderlyAppointmentRequest;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.DoctorScheduleMapper;
import com.community.smartelderlybackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老人端与家属端共用的预约创建逻辑，避免重复与越权不一致。
 */
@Service
public class ElderlyAppointmentService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;

    public Result<Map<String, Object>> createPendingAppointment(ElderlyAppointmentRequest request) {
        if (request == null || request.getDoctorId() == null) {
            return Result.error("doctorId 不能为空");
        }
        if (request.getUserId() == null) {
            return Result.error("userId 不能为空");
        }

        User elder = userMapper.selectById(request.getUserId());
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (elder.getRole() == null || elder.getRole() != 0) {
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

        if (schedule.getDoctorId() == null || !schedule.getDoctorId().equals(request.getDoctorId())) {
            return Result.error("排班与医生不匹配");
        }

        LocalDateTime targetAppointTime = resolveAppointTime(schedule, request.getAppointTime());

        // 只禁止“同一老人同一时段”重复预约（不再限制同一医生的不同时间预约）
        Long timeConflictCount = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, request.getUserId())
                .in(Appointment::getStatus, 0, 1)
                .eq(Appointment::getAppointTime, targetAppointTime));
        if (timeConflictCount != null && timeConflictCount > 0) {
            return Result.error("该时段已有进行中的预约，请选择其他时段");
        }

        int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
        int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();
        int timeSlot = schedule.getTimeSlot() == null ? 0 : schedule.getTimeSlot();
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
        appointment.setAppointTime(targetAppointTime);
        appointmentMapper.insert(appointment);

        Map<String, Object> result = new HashMap<>();
        result.put("appointId", appointment.getAppointId());
        result.put("status", appointment.getStatus());
        result.put("appointTime", appointment.getAppointTime());
        return Result.success(result);
    }

    /**
     * 某医生从指定日期起的排班列表（含剩余号源），供选日期预约。
     */
    public List<Map<String, Object>> listSchedulesForDoctor(Long doctorId, LocalDate fromDate) {
        if (doctorId == null || fromDate == null) {
            return List.of();
        }
        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .ge(DoctorSchedule::getWorkDate, fromDate)
                .orderByAsc(DoctorSchedule::getWorkDate)
                .orderByAsc(DoctorSchedule::getTimeSlot));
        if (schedules.isEmpty()) {
            return List.of();
        }
        LocalDate maxDate = schedules.stream()
                .map(DoctorSchedule::getWorkDate)
                .max(LocalDate::compareTo)
                .orElse(fromDate);
        LocalDateTime rangeStart = fromDate.atStartOfDay();
        LocalDateTime rangeEnd = maxDate.atTime(LocalTime.MAX);
        List<Appointment> pendingAppts = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getStatus, 0)
                .between(Appointment::getAppointTime, rangeStart, rangeEnd));

        List<Map<String, Object>> out = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            Map<String, Object> map = new HashMap<>();
            User doctor = userMapper.selectById(schedule.getDoctorId());
            int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
            int maxCapacity = schedule.getMaxCapacity() == null ? 0 : schedule.getMaxCapacity();
            int timeSlot = schedule.getTimeSlot() == null ? 0 : schedule.getTimeSlot();
            LocalDate workDate = schedule.getWorkDate();
            LocalDateTime start = workDate.atStartOfDay();
            LocalDateTime end = workDate.atTime(LocalTime.MAX);
            long pendingCount = pendingAppts.stream()
                    .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(schedule.getDoctorId()))
                    .filter(a -> a.getAppointTime() != null)
                    .filter(a -> !a.getAppointTime().isBefore(start) && !a.getAppointTime().isAfter(end))
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
            out.add(map);
        }
        return out;
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
}
