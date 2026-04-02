-- 1. 创建数据库并使用
CREATE DATABASE IF NOT EXISTS smart_elderly_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_elderly_db;

-- ==========================================
-- 2. 创建核心表结构
-- ==========================================

-- 表1：用户信息表 (独立表，最先创建)
CREATE TABLE `user` (
  `user_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '登录密码(加密)',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `age` INT(3) DEFAULT NULL COMMENT '年龄',
  `role` TINYINT(1) DEFAULT NULL COMMENT '角色(0老人,1家属,2医生,3社区)',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 表2：健康档案表
CREATE TABLE `health_records` (
  `record_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录唯一标识',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '所属用户ID(老人)',
  `blood_pressure_high` FLOAT(4,1) DEFAULT NULL COMMENT '收缩压(高压)',
  `blood_pressure_low` FLOAT(4,1) DEFAULT NULL COMMENT '舒张压(低压)',
  `heart_rate` INT(3) DEFAULT NULL COMMENT '心率',
  `blood_sugar` FLOAT(4,1) DEFAULT NULL COMMENT '血糖',
  `medication_info` TEXT COMMENT '用药信息(用于语音播报)',
  `record_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`record_id`),
  KEY `fk_health_user` (`user_id`),
  CONSTRAINT `fk_health_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案表';

-- 表3：医生排班表
CREATE TABLE `doctor_schedule` (
  `schedule_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '排班唯一标识',
  `doctor_id` BIGINT(20) DEFAULT NULL COMMENT '医生ID',
  `work_date` DATE DEFAULT NULL COMMENT '排班日期',
  `time_slot` TINYINT(1) DEFAULT NULL COMMENT '预约时段(0上午,1下午)',
  `max_capacity` INT(4) DEFAULT NULL COMMENT '最大号源数',
  `booked_count` INT(4) DEFAULT 0 COMMENT '已预约数',
  PRIMARY KEY (`schedule_id`),
  KEY `fk_schedule_doctor` (`doctor_id`),
  CONSTRAINT `fk_schedule_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班表';

-- 表4：预约记录表
CREATE TABLE `appointment` (
  `appoint_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '预约唯一标识',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '预约患者ID',
  `doctor_id` BIGINT(20) DEFAULT NULL COMMENT '接诊医生ID',
  `appoint_time` DATETIME DEFAULT NULL COMMENT '预约服务时间',
  `status` TINYINT(1) DEFAULT 0 COMMENT '预约状态(0待处理,1已确认,2已取消)',
  PRIMARY KEY (`appoint_id`),
  KEY `fk_appoint_user` (`user_id`),
  KEY `fk_appoint_doctor` (`doctor_id`),
  CONSTRAINT `fk_appoint_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_appoint_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- 表5：紧急求助记录表
CREATE TABLE `emergency_record` (
  `help_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '求助唯一标识',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '求助老人ID',
  `help_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '求助发生时间',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '位置/地址信息',
  `status` TINYINT(1) DEFAULT 0 COMMENT '处理状态(0待处理,1家属接单,2社区接单,3已解决)',
  `handle_result` TEXT COMMENT '处理结果说明',
  PRIMARY KEY (`help_id`),
  KEY `fk_emergency_user` (`user_id`),
  CONSTRAINT `fk_emergency_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='紧急求助记录表';

-- 表6：家属绑定关系表
CREATE TABLE `family_bind` (
  `bind_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '绑定唯一标识',
  `elder_id` BIGINT(20) DEFAULT NULL COMMENT '老年人ID',
  `family_id` BIGINT(20) DEFAULT NULL COMMENT '家属ID',
  `relation` VARCHAR(50) DEFAULT NULL COMMENT '亲属关系',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  PRIMARY KEY (`bind_id`),
  KEY `fk_bind_elder` (`elder_id`),
  KEY `fk_bind_family` (`family_id`),
  CONSTRAINT `fk_bind_elder` FOREIGN KEY (`elder_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bind_family` FOREIGN KEY (`family_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属绑定关系表';

-- ==========================================
-- 3. 插入初始测试数据 (MVP展示必备)
-- ==========================================

INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `age`, `role`, `phone`) VALUES 
(1001, 'laowang', '123456', '王大爷', 75, 0, '13800001111'),
(1002, 'dr_li', '123456', '李医生', 40, 2, '13900002222');

INSERT INTO `health_records` (`user_id`, `blood_pressure_high`, `blood_pressure_low`, `heart_rate`, `blood_sugar`, `medication_info`) VALUES 
(1001, 135.0, 85.0, 72, 5.5, '阿司匹林，每日一次');

INSERT INTO `doctor_schedule` (`doctor_id`, `work_date`, `time_slot`, `max_capacity`, `booked_count`) VALUES 
(1002, CURDATE() + INTERVAL 1 DAY, 0, 10, 0);