package com.community.smartelderlybackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求参数")
public class LoginRequest {
    @Schema(description = "登录账号", example = "laowang")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    private String password;

    @Schema(description = "角色（0老人，1家属，2医生，3社区）", example = "0")
    private Integer role;
}
