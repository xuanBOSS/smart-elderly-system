package com.community.smartelderlybackend.dto;

import lombok.Data;

@Data
public class EmergencyRequest {
    private Long userId;
    private String location;
    private Double latitude;
    private Double longitude;
}
