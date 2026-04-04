package com.community.smartelderlybackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.smartelderlybackend.entity.User;
import com.community.smartelderlybackend.mapper.UserMapper;
import com.community.smartelderlybackend.service.UserService;
import org.springframework.stereotype.Service;

@Service // 告诉系统这是一个主厨（服务类）
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password, Integer role) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username)
                .eq(User::getPassword, password)
                .eq(User::getRole, role)
                .last("LIMIT 1");
        return getOne(queryWrapper, false);
    }
}