package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diagnosis_record")
public class DiagnosisRecord {
    @TableId(type = IdType.AUTO)
    private Long diagnosisId;
    private Long userId;      // 老人ID
    private Long doctorId;    // 诊断医生ID，可为空
    private String diagnosisType; // 高血压/低血压/糖尿病/心率异常
    private String note;
    private Integer active;   // 1有效 0失效
    private LocalDateTime diagnosisTime;
}

