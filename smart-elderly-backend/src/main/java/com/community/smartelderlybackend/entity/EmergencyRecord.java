package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emergency_record")
public class EmergencyRecord {
    @TableId(type = IdType.AUTO)
    private Long helpId;
    private Long userId;
    private LocalDateTime helpTime;
    private String location;
    private Integer status;
    private String handleResult;
}
