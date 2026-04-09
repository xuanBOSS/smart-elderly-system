package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("health_records")
public class HealthRecords {
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long userId; // 老人的ID
    private Float bloodPressureHigh; // 收缩压
    private Float bloodPressureLow;  // 舒张压
    private Integer heartRate;
    private Float bloodSugar;
    private String medicationInfo;
    private LocalDateTime recordTime;
}