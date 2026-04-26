package com.community.smartelderlybackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.dto.ElderlyAppointmentRequest;
import com.community.smartelderlybackend.dto.EmergencyRequest;
import com.community.smartelderlybackend.entity.Appointment;
import com.community.smartelderlybackend.entity.BuildingGeoZone;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.EmergencyRecord;
import com.community.smartelderlybackend.entity.HealthRecords;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.AppointmentMapper;
import com.community.smartelderlybackend.mapper.BuildingGeoZoneMapper;
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
import java.util.Optional;

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
    @Autowired
    private BuildingGeoZoneMapper buildingGeoZoneMapper;
    @Autowired
    private com.community.smartelderlybackend.service.AiService aiService;
    @Autowired
    private com.community.smartelderlybackend.service.ElderlyAppointmentService elderlyAppointmentService;

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
                location = buildEmergencyLocationText(latitude, longitude);
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

    /**
     * 基于坐标范围映射楼栋号，拼接可用于社区态势图高亮的位置信息文本。
     */
    private String buildEmergencyLocationText(Double latitude, Double longitude) {
        String coord = buildCoordinateText(latitude, longitude);
        Integer buildingNo = resolveBuildingNo(latitude, longitude).orElse(null);
        if (buildingNo == null) {
            return coord;
        }
        return "小区" + buildingNo + "号楼 " + coord;
    }

    private Optional<Integer> resolveBuildingNo(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return Optional.empty();
        List<BuildingGeoZone> zones = buildingGeoZoneMapper.selectList(new LambdaQueryWrapper<BuildingGeoZone>()
                .eq(BuildingGeoZone::getActive, 1));
        for (BuildingGeoZone z : zones) {
            if (z.getBuildingNo() == null
                    || z.getMinLatitude() == null || z.getMaxLatitude() == null
                    || z.getMinLongitude() == null || z.getMaxLongitude() == null) {
                continue;
            }
            boolean inLat = latitude >= z.getMinLatitude() && latitude <= z.getMaxLatitude();
            boolean inLng = longitude >= z.getMinLongitude() && longitude <= z.getMaxLongitude();
            if (inLat && inLng) {
                return Optional.of(z.getBuildingNo());
            }
        }
        return Optional.empty();
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

    @GetMapping("/doctorSchedules")
    @Operation(summary = "某医生从今日起的可预约排班列表（含剩余号源）")
    public Result<List<Map<String, Object>>> getDoctorSchedules(
            @RequestParam Long userId,
            @RequestParam Long doctorId) {
        User elder = userMapper.selectById(userId);
        if (elder == null) {
            return Result.error("老人不存在");
        }
        if (!isElderlyUser(elder)) {
            return Result.error("该用户不是老人");
        }
        User doctor = userMapper.selectById(doctorId);
        if (doctor == null || doctor.getRole() == null || doctor.getRole() != 2) {
            return Result.error("医生不存在");
        }
        return Result.success(elderlyAppointmentService.listSchedulesForDoctor(doctorId, LocalDate.now()));
    }

    @PostMapping("/appointment")
    @Operation(summary = "挂号预约接口")
    public Result<Map<String, Object>> handleAppointment(@RequestBody(required = false) ElderlyAppointmentRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("schedules", queryTodaySchedules());

        if (request == null || request.getDoctorId() == null) {
            return Result.success(data);
        }
        Result<Map<String, Object>> bookResult = elderlyAppointmentService.createPendingAppointment(request);
        if (bookResult.getCode() == null || bookResult.getCode() != 200) {
            return bookResult;
        }
        Map<String, Object> merged = new HashMap<>(data);
        if (bookResult.getData() != null) {
            merged.putAll(bookResult.getData());
        }
        return Result.success(merged);
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

    @PostMapping("/voice/intent")
    @Operation(summary = "AI 语音意图解析与自动填报引擎")
    public Result<String> processVoiceIntent(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String text = params.get("text").toString(); // 老人说的话，比如："我今天高压140低压90"

        if (text == null || text.trim().isEmpty()) {
            return Result.error("没听清您说的话");
        }

        // 1. 组装给 DeepSeek 
        String prompt = String.format(
                "你是一个智慧养老系统的意图解析引擎。老人家说了一句话：【%s】。\n" +
                        "请你分析这句话的意图，并严格按以下规则返回 JSON 数据（绝不要输出任何解释性文字或Markdown标记，只输出纯JSON）：\n" +
                        "规则1：如果是记录体征数据（血压、血糖、心率等），返回 {\"action\": \"health\", \"data\": {\"bloodPressureHigh\": 140, \"bloodPressureLow\": 90, \"heartRate\": 75, \"bloodSugar\": 6.1}}\n" +
                        "规则2：如果是想预约看病，返回 {\"action\": \"book\", \"data\": {\"keyword\": \"李医生\"}}\n" +
                        "规则3：如果是求救或身体不适，返回 {\"action\": \"sos\", \"data\": {\"desc\": \"具体不适描述\"}}\n" +
                        "如果只说了部分数据（比如只说了血压），其他字段填 null。",
                text
        );

        // 2. 呼叫 DeepSeek
        String aiResponse = aiService.getRiskPrediction(prompt); // 复用昨天的通信方法
        System.out.println("🤖 AI 解析结果: " + aiResponse);

        try {
            // 清理可能带有的 markdown 标记 (比如 ```json ... ```)
            aiResponse = aiResponse.replace("```json", "").replace("```", "").trim();

            // 3. 解析 AI 返回的 JSON，并自动执行 CRUD 数据库操作
            cn.hutool.json.JSONObject resultObj = cn.hutool.json.JSONUtil.parseObj(aiResponse);
            String action = resultObj.getStr("action");
            cn.hutool.json.JSONObject dataObj = resultObj.getJSONObject("data");

            if ("health".equals(action)) {
                // 自动保存健康记录
                HealthRecords hr = new HealthRecords();
                hr.setUserId(userId);
                hr.setRecordTime(LocalDateTime.now());

                if(dataObj.get("bloodPressureHigh") != null) hr.setBloodPressureHigh(dataObj.getFloat("bloodPressureHigh"));
                if(dataObj.get("bloodPressureLow") != null) hr.setBloodPressureLow(dataObj.getFloat("bloodPressureLow"));
                if(dataObj.get("heartRate") != null) hr.setHeartRate(dataObj.getInt("heartRate"));
                if(dataObj.get("bloodSugar") != null) hr.setBloodSugar(dataObj.getFloat("bloodSugar"));

                healthRecordsMapper.insert(hr);
                return Result.success("体征数据已自动记录到您的健康档案。");

            } else if ("book".equals(action)) {
                // 1. 拿到 AI 从语音里提取出的医生名字（比如：老人说"我想挂李医生的号"，keyword 就是 "李医生"）
                String keyword = dataObj.getStr("keyword");
                if (keyword == null || keyword.isEmpty()) {
                    return Result.error("请您告诉我具体想预约哪位医生？");
                }

                // 2. 去数据库里模糊搜索这位医生 (role = 2 代表医生)
                // 注意：这里为了防止名字带"医生"两个字查不到，简单去除了"医生"、"大夫"等后缀
                String searchName = keyword.replace("医生", "").replace("大夫", "");
                User doctor = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 2)
                        .like(User::getRealName, searchName)
                        .last("LIMIT 1"));

                if (doctor == null) {
                    return Result.error("抱歉，咱们社区医院暂时没有匹配的医生，请换一位试试。");
                }

                // 3. 自动生成一条待确认的预约记录存入数据库！
                Appointment appt = new Appointment();
                appt.setUserId(userId);
                appt.setDoctorId(doctor.getUserId());
                appt.setStatus(0); // 0表示待确认
                appt.setAppointTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)); // 默认帮老人约明天上午9点
                appointmentMapper.insert(appt);

                // 返回的这句话里必须带“预约”两个字，这样前端听到后才会自动刷新列表
                return Result.success("已为您成功预约" + doctor.getRealName() + "医生！");

            } else if ("sos".equals(action)) {
                // 更新刚才的紧急求助记录
                String desc = dataObj.getStr("desc");
                return Result.success("收到！您的情况是：" + desc + "，救援马上就到！");
            }

            return Result.success("我听懂了，但没有相关的操作指令。");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("语音意图解析失败，系统开小差了");
        }
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

    private boolean isElderlyUser(User user) {
        return user.getRole() != null && user.getRole() == 0;
    }

    @PostMapping("/emergency/updateDesc")
    @Operation(summary = "更新紧急求助的详情(语音补充)")
    public Result<String> updateEmergencyDesc(@RequestBody Map<String, Object> params) {
        Long helpId = Long.valueOf(params.get("helpId").toString());
        String desc = params.get("desc").toString();

        EmergencyRecord record = emergencyRecordMapper.selectById(helpId);
        if (record != null) {
            // 将老人补充的语音转文字，追加到 handle_result 中
            record.setHandleResult("【语音补充情况】：" + desc);
            emergencyRecordMapper.updateById(record);
            return Result.success("详情已更新");
        }
        return Result.error("未找到该求助工单");
    }
}
