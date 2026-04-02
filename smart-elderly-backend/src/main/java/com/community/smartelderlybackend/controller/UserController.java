package com.community.smartelderlybackend.controller;

import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.service.UserService;
// 导入 Swagger 的注解
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户模块", description = "包含用户的登录、注册、查询等所有接口") // 🌟 给整个控制器起个中文名
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询老人信息", description = "传入老人的专属ID，返回详细的档案数据") // 🌟 给这个接口起个中文说明
    public Result<User> getUserById(
            @Parameter(description = "用户的唯一ID号") @PathVariable Long id) { // 🌟 给参数加个中文解释

        User user = userService.getById(id);

        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("未找到该用户");
        }
    }
}