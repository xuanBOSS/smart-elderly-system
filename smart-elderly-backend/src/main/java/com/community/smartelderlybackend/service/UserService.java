package com.community.smartelderlybackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.smartelderlybackend.entity.User;

public interface UserService extends IService<User> {
    /**
     * 按账号、密码、角色进行登录校验。
     *
     * @param username 登录账号
     * @param password 登录密码
     * @param role     角色（0老人，1家属，2医生，3社区）
     * @return 命中的用户；未命中返回 null
     */
    User login(String username, String password, Integer role);
}