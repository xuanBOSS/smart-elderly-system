package com.community.smartelderlybackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ElderlyAppointmentRequest {
    private Long userId;
    private Long doctorId;
    private Long scheduleId;
    private LocalDateTime appointTime;
}
