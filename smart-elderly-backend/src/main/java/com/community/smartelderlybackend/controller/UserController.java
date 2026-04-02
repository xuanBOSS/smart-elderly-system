package com.community.smartelderlybackend.controller;

import com.community.smartelderlybackend.common.Result;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin // 极其重要：允许胡静雨的 Vue 前端跨域访问这个接口！
@RestController
@RequestMapping("/api/user") // 餐厅的招牌，这个控制器管理所有 /api/user 开头的请求
public class UserController {

    @Autowired
    private UserService userService; // 把主厨叫过来

    // 定义具体接口：根据用户 ID 查询信息
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        // 调用主厨的方法去查数据库
        User user = userService.getById(id);

        if (user != null) {
            // 如果查到了，用万能盒子包起来返回成功
            return Result.success(user);
        } else {
            // 如果没查到，返回失败提示
            return Result.error("未找到该用户");
        }
    }
}