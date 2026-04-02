package com.community.smartelderlybackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.smartelderlybackend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉系统这是一个去查数据库的 Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承了 BaseMapper，MyBatis-Plus 已经帮你把增删改查全写好了，这里空着就行！
}