package com.community.smartelderlybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.smartelderlybackend.entity.DoctorSchedule;
import com.community.smartelderlybackend.entity.DoctorWorkPattern;
import com.community.smartelderlybackend.mapper.DoctorScheduleMapper;
import com.community.smartelderlybackend.mapper.DoctorWorkPatternMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DoctorScheduleAutoGenerateService {
    private static final Logger log = LoggerFactory.getLogger(DoctorScheduleAutoGenerateService.class);
    private static final int FUTURE_DAYS = 30;

    @Autowired
    private DoctorWorkPatternMapper doctorWorkPatternMapper;
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;

    @PostConstruct
    public void initOnStartup() {
        ensureFutureSchedules();
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void scheduledGenerate() {
        ensureFutureSchedules();
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureFutureSchedules() {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(FUTURE_DAYS - 1L);

        List<DoctorWorkPattern> patterns = doctorWorkPatternMapper.selectList(
                new LambdaQueryWrapper<DoctorWorkPattern>()
                        .eq(DoctorWorkPattern::getActive, 1)
                        .orderByAsc(DoctorWorkPattern::getDoctorId)
                        .orderByAsc(DoctorWorkPattern::getWeekday)
                        .orderByAsc(DoctorWorkPattern::getTimeSlot)
        );
        if (patterns.isEmpty()) {
            log.warn("doctor_work_pattern 为空，跳过自动排班生成");
            return;
        }

        List<DoctorSchedule> existing = doctorScheduleMapper.selectList(
                new LambdaQueryWrapper<DoctorSchedule>()
                        .between(DoctorSchedule::getWorkDate, from, to)
        );
        Set<String> occupied = new HashSet<>();
        for (DoctorSchedule s : existing) {
            if (s.getDoctorId() == null || s.getWorkDate() == null || s.getTimeSlot() == null) {
                continue;
            }
            occupied.add(keyOf(s.getDoctorId(), s.getWorkDate(), s.getTimeSlot()));
        }

        int inserted = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            int weekday = d.getDayOfWeek().getValue(); // 1-7 => 周一到周日
            for (DoctorWorkPattern p : patterns) {
                if (p.getDoctorId() == null || p.getWeekday() == null || p.getTimeSlot() == null) {
                    continue;
                }
                if (p.getWeekday() != weekday) {
                    continue;
                }
                String key = keyOf(p.getDoctorId(), d, p.getTimeSlot());
                if (occupied.contains(key)) {
                    continue;
                }
                DoctorSchedule schedule = new DoctorSchedule();
                schedule.setDoctorId(p.getDoctorId());
                schedule.setWorkDate(d);
                schedule.setTimeSlot(p.getTimeSlot());
                int capacity = p.getDefaultCapacity() == null ? 10 : Math.max(p.getDefaultCapacity(), 1);
                schedule.setMaxCapacity(capacity);
                schedule.setBookedCount(0);
                doctorScheduleMapper.insert(schedule);
                occupied.add(key);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("自动排班完成：新增 {} 条，覆盖区间 {} ~ {}", inserted, from, to);
        } else {
            log.info("自动排班检查完成：无需新增，区间 {} ~ {}", from, to);
        }
    }

    private String keyOf(Long doctorId, LocalDate workDate, Integer timeSlot) {
        return doctorId + "|" + workDate + "|" + timeSlot;
    }
}
