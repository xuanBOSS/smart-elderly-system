package com.community.smartelderlybackend.controller;

import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.service.UserService;
import com.community.smartelderlybackend.utils.JwtUtils; // 🌟 新增：导入刚刚写的 JWT 制造机

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
    @Operation(summary = "用户登录联调测试", description = "A同学专用测试接口，测试账号: admin, 密码: 123")
    public Result<String> login(
            @Parameter(description = "账号") @RequestParam String username,
            @Parameter(description = "密码") @RequestParam String password) {

        // 这是你写的假逻辑，用来跑通全链路。之后 C 同学会把这里换成查数据库的真实逻辑
        if ("admin".equals(username) && "123".equals(password)) {
            // 发放令牌：假设这是个社区管理员，数据库 ID 是 1，角色是 3
            String token = JwtUtils.generateToken(1L, 3);
            return Result.success(token);
        }
        return Result.error("账号或密码错误");
    }

    @GetMapping("/testToken")
    @Operation(summary = "测试保安拦截器", description = "如果不带 Token 请求这个接口，会被直接踢出去")
    public Result<String> testToken() {
        return Result.success("恭喜！Token校验成功，全链路打通！");
    }
}