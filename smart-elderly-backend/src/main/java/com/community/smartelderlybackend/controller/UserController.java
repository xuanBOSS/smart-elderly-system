package com.community.smartelderlybackend.controller;

import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.dto.LoginRequest;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.service.UserService;
import com.community.smartelderlybackend.utils.JwtUtils; // 🌟 新增：导入刚刚写的 JWT 制造机
import com.community.smartelderlybackend.vo.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin (因为有了全局配置，这里不需要了)
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户模块", description = "包含用户的登录、注册、查询等所有接口")
public class UserController {

    @Autowired
    private UserService userService;

    // --- 这是你原来写好的接口，原封不动保留，给 C 同学留着 ---
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询老人信息", description = "传入老人的专属ID，返回详细的档案数据")
    public Result<User> getUserById(
            @Parameter(description = "用户的唯一ID号") @PathVariable Long id) {

        User user = userService.getById(id);

        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("未找到该用户");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持老人(0)、家属(1)、医生(2)、社区(3)四类角色登录")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword()) || request.getRole() == null) {
            return Result.error("请完整填写账号、密码和角色");
        }

        if (request.getRole() < 0 || request.getRole() > 3) {
            return Result.error("角色参数非法");
        }

        User user = userService.login(request.getUsername(), request.getPassword(), request.getRole());
        if (user == null) {
            return Result.error("账号、密码或角色错误");
        }

        String token = JwtUtils.generateToken(user.getUserId(), user.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setRoleName(getRoleName(user.getRole()));

        return Result.success(response);
    }

    @GetMapping("/testToken")
    @Operation(summary = "测试保安拦截器", description = "如果不带 Token 请求这个接口，会被直接踢出去")
    public Result<String> testToken() {
        return Result.success("恭喜！Token校验成功，全链路打通！");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getRoleName(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case 0 -> "老人";
            case 1 -> "家属";
            case 2 -> "医生";
            case 3 -> "社区";
            default -> "未知";
        };
    }
}