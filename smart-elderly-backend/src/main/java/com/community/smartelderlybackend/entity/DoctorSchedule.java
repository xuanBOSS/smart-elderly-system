package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("doctor_schedule")
public class DoctorSchedule {
    @TableId(type = IdType.AUTO)
    private Long scheduleId;
    private Long doctorId;
    private LocalDate workDate;
    private Integer timeSlot; // 0上午, 1下午
    private Integer maxCapacity;
    private Integer bookedCount;
}