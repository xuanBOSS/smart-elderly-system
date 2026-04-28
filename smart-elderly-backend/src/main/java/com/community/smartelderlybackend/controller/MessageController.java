package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.FamilyBind;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.FamilyBindMapper;
import com.community.smartelderlybackend.mapper.EmergencyRecordMapper;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
@Tag(name = "消息通知模块")
public class MessageController {

    @Autowired
    private EmergencyRecordMapper emergencyRecordMapper;
    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private HealthRecordsMapper healthRecordsMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FamilyBindMapper familyBindMapper;

    @GetMapping("/list")
    @Operation(summary = "获取消息通知列表")
    public Result<List<Map<String, Object>>> list(@RequestParam Long elderId, HttpServletRequest request) {
        Long familyId = request.getAttribute("userId") instanceof Number n ? n.longValue() : null;
        Long bound = familyBindMapper.selectCount(new LambdaQueryWrapper<FamilyBind>()
                .eq(FamilyBind::getFamilyId, familyId)
                .eq(FamilyBind::getElderId, elderId));
        if (bound == null || bound == 0) {
            return Result.error("您未绑定该老人或无权查看消息");
        }

        List<NoticeItem> allNotices = new ArrayList<>();
        User elder = userMapper.selectById(elderId);
        String elderName = elder != null ? elder.getRealName() : "该老人";

        LambdaQueryWrapper<EmergencyRecord> emergencyWrapper = new LambdaQueryWrapper<>();
        emergencyWrapper.eq(EmergencyRecord::getUserId, elderId)
                .orderByDesc(EmergencyRecord::getHelpTime)
                .last("LIMIT 5");
        List<EmergencyRecord> emergencyRecords = emergencyRecordMapper.selectList(emergencyWrapper);
        for (EmergencyRecord record : emergencyRecords) {
            allNotices.add(new NoticeItem(
                    record.getHelpId(),
                    "URGENT",
                    elderName + "触发了紧急求助",
                    record.getHelpTime()
            ));
        }

        LambdaQueryWrapper<Appointment> appointmentWrapper = new LambdaQueryWrapper<>();
        appointmentWrapper.eq(Appointment::getUserId, elderId)
                .eq(Appointment::getStatus, 1)
                .orderByDesc(Appointment::getAppointTime)
                .last("LIMIT 5");
        List<Appointment> appointments = appointmentMapper.selectList(appointmentWrapper);
        LocalDateTime now = LocalDateTime.now();
        int idx = 0;
        for (Appointment appointment : appointments) {
            User doctor = userMapper.selectById(appointment.getDoctorId());
            String doctorName = doctor != null ? doctor.getRealName() : "医生";
            String dateText = appointment.getAppointTime() != null
                    ? appointment.getAppointTime().format(DateTimeFormatter.ofPattern("MM/dd"))
                    : "--/--";
            allNotices.add(new NoticeItem(
                    appointment.getAppointId() + 100000,
                    "INFO",
                    "门诊预约已确认（" + doctorName + " " + dateText + "）",
                    // appointment 仅记录就诊时间，提醒时间用“当前时间”更贴近实时消息体验
                    now.minusSeconds(idx++)
            ));
        }

        LambdaQueryWrapper<HealthRecords> healthWrapper = new LambdaQueryWrapper<>();
        healthWrapper.eq(HealthRecords::getUserId, elderId)
                .orderByDesc(HealthRecords::getRecordTime)
                .last("LIMIT 1");
        HealthRecords latest = healthRecordsMapper.selectOne(healthWrapper);
        if (latest != null && isWarning(latest)) {
            allNotices.add(new NoticeItem(
                    latest.getRecordId() + 200000,
                    "WARNING",
                    buildWarningText(latest),
                    latest.getRecordTime()
            ));
        }

        allNotices.sort(Comparator.comparing(NoticeItem::getSortTime, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> response = new ArrayList<>();
        for (NoticeItem item : allNotices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.id);
            map.put("type", item.type);
            map.put("content", item.content);
            map.put("time", formatTime(item.sortTime));
            response.add(map);
            if (response.size() >= 20) {
                break;
            }
        }
        return Result.success(response);
    }

    private boolean isWarning(HealthRecords latest) {
        return (latest.getBloodPressureHigh() != null && latest.getBloodPressureHigh() >= 140)
                || (latest.getBloodPressureLow() != null && latest.getBloodPressureLow() >= 90)
                || (latest.getBloodSugar() != null && latest.getBloodSugar() >= 7);
    }

    private String buildWarningText(HealthRecords latest) {
        if (latest.getBloodPressureHigh() != null && latest.getBloodPressureHigh() >= 140) {
            return "血压偏高预警提醒";
        }
        if (latest.getBloodPressureLow() != null && latest.getBloodPressureLow() >= 90) {
            return "舒张压偏高预警提醒";
        }
        return "血糖偏高预警提醒";
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "--:--";
        }
        return time.format(DateTimeFormatter.ofPattern("MM/dd HH:mm:ss"));
    }

    private static class NoticeItem {
        private final Long id;
        private final String type;
        private final String content;
        private final LocalDateTime sortTime;

        private NoticeItem(Long id, String type, String content, LocalDateTime sortTime) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.sortTime = sortTime;
        }

        public LocalDateTime getSortTime() {
            return sortTime;
        }
    }
}
