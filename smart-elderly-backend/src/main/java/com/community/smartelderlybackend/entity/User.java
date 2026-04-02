package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user") // 告诉系统，这个类对应数据库的 user 表
public class User {

    @TableId(value = "user_id", type = IdType.AUTO) // 告诉系统，user_id 是主键
    private Long userId;

    private String username;
    private String password;
    private String realName;
    private Integer age;
    private Integer role;
    private String phone;
}