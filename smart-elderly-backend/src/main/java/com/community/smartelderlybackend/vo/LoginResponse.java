package com.community.smartelderlybackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录返回信息")
public class LoginResponse {
    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String realName;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "角色（0老人，1家属，2医生，3社区）")
    private Integer role;

    @Schema(description = "角色名称")
    private String roleName;
}
