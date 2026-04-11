package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long appointId;
    private Long userId; // 患者(老人)ID
    private Long doctorId; // 医生ID
    private LocalDateTime appointTime;
    private Integer status; // 0待处理, 1已确认, 2已取消/拒绝
}