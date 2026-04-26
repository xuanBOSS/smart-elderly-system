SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `appoint_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约唯一标识',
  `user_id` bigint NULL DEFAULT NULL COMMENT '预约患者ID',
  `doctor_id` bigint NULL DEFAULT NULL COMMENT '接诊医生ID',
  `appoint_time` datetime NULL DEFAULT NULL COMMENT '预约服务时间',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '预约状态(0待处理,1已确认,2已取消)',
  PRIMARY KEY (`appoint_id`) USING BTREE,
  INDEX `fk_appoint_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_appoint_doctor`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `fk_appoint_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_appoint_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appointment 
-- ----------------------------
INSERT INTO `appointment` VALUES 
(1, 1021, 1001, '2026-04-04 09:00:00', 1),
(2, 1022, 1002, '2026-04-04 14:30:00', 1),
(3, 1023, 1003, '2026-04-05 08:30:00', 1),
(4, 1024, 1001, '2026-04-05 15:00:00', 0),
(5, 1025, 1004, '2026-04-06 09:00:00', 1),
(6, 1026, 1002, '2026-04-06 16:00:00', 2),
(7, 1027, 1005, '2026-04-07 08:30:00', 1),
(8, 1028, 1001, '2026-04-07 10:00:00', 0),
(9, 1029, 1006, '2026-04-08 09:30:00', 1),
(10, 1030, 1003, '2026-04-08 14:00:00', 1),
(11, 1031, 1007, '2026-04-09 08:30:00', 0),
(12, 1032, 1008, '2026-04-09 15:30:00', 1),
(13, 1033, 1009, '2026-04-10 09:00:00', 1),
(14, 1034, 1010, '2026-04-10 16:30:00', 2),
(15, 1035, 1004, '2026-04-11 08:30:00', 0),
(16, 1036, 1005, '2026-04-11 10:30:00', 1),
(17, 1037, 1006, '2026-04-12 09:00:00', 1),
(18, 1038, 1002, '2026-04-12 14:30:00', 0),
(19, 1039, 1001, '2026-04-13 08:30:00', 1),
(20, 1040, 1007, '2026-04-13 15:00:00', 1);

-- ----------------------------
-- Table structure for doctor_work_pattern
-- ----------------------------
DROP TABLE IF EXISTS `doctor_work_pattern`;
CREATE TABLE `doctor_work_pattern`  (
  `pattern_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `doctor_id` bigint NOT NULL COMMENT '医生ID',
  `weekday` tinyint NOT NULL COMMENT '周几(1=周一 ... 7=周日)',
  `time_slot` tinyint NOT NULL COMMENT '时段(0上午,1下午)',
  `default_capacity` int NOT NULL DEFAULT 10 COMMENT '默认号源',
  `active` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用(1启用,0停用)',
  PRIMARY KEY (`pattern_id`) USING BTREE,
  UNIQUE KEY `uk_work_pattern` (`doctor_id`,`weekday`,`time_slot`),
  CONSTRAINT `fk_work_pattern_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生固定班次模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_work_pattern
-- ----------------------------
INSERT INTO `doctor_work_pattern` VALUES (1, 1001, 1, 0, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (2, 1001, 3, 0, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (3, 1001, 5, 1, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (4, 1002, 1, 0, 25, 1);
INSERT INTO `doctor_work_pattern` VALUES (5, 1002, 3, 0, 25, 1);
INSERT INTO `doctor_work_pattern` VALUES (6, 1002, 5, 1, 25, 1);
INSERT INTO `doctor_work_pattern` VALUES (7, 1003, 2, 0, 10, 1);
INSERT INTO `doctor_work_pattern` VALUES (8, 1003, 4, 0, 10, 1);
INSERT INTO `doctor_work_pattern` VALUES (9, 1004, 1, 1, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (10, 1004, 3, 0, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (11, 1005, 2, 0, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (12, 1005, 4, 1, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (13, 1006, 1, 0, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (14, 1006, 4, 1, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (15, 1007, 2, 0, 12, 1);
INSERT INTO `doctor_work_pattern` VALUES (16, 1007, 5, 0, 12, 1);
INSERT INTO `doctor_work_pattern` VALUES (17, 1008, 1, 1, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (18, 1008, 3, 0, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (19, 1009, 2, 1, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (20, 1009, 4, 0, 20, 1);
INSERT INTO `doctor_work_pattern` VALUES (21, 1010, 1, 0, 15, 1);
INSERT INTO `doctor_work_pattern` VALUES (22, 1010, 3, 1, 15, 1);

-- ----------------------------
-- Table structure for doctor_schedule
-- ----------------------------
DROP TABLE IF EXISTS `doctor_schedule`;
CREATE TABLE `doctor_schedule`  (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班唯一标识',
  `doctor_id` bigint NULL DEFAULT NULL COMMENT '医生ID',
  `work_date` date NULL DEFAULT NULL COMMENT '排班日期',
  `time_slot` tinyint(1) NULL DEFAULT NULL COMMENT '预约时段(0上午,1下午)',
  `max_capacity` int NULL DEFAULT NULL COMMENT '最大号源数',
  `booked_count` int NULL DEFAULT 0 COMMENT '已预约数',
  PRIMARY KEY (`schedule_id`) USING BTREE,
  INDEX `fk_schedule_doctor`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `fk_schedule_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生排班表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_schedule
-- ----------------------------
INSERT INTO `doctor_schedule` VALUES (1, 1002, '2026-04-04', 0, 10, 0);
INSERT INTO `doctor_schedule` VALUES (2, 1001, '2026-04-04', 0, 15, 10);
INSERT INTO `doctor_schedule` VALUES (3, 1001, '2026-04-05', 1, 15, 6);
INSERT INTO `doctor_schedule` VALUES (4, 1001, '2026-04-07', 0, 15, 12);
INSERT INTO `doctor_schedule` VALUES (5, 1001, '2026-04-09', 1, 15, 15);
INSERT INTO `doctor_schedule` VALUES (6, 1001, '2026-04-12', 0, 15, 3);
INSERT INTO `doctor_schedule` VALUES (7, 1002, '2026-04-04', 1, 25, 18);
INSERT INTO `doctor_schedule` VALUES (8, 1002, '2026-04-06', 0, 25, 25);
INSERT INTO `doctor_schedule` VALUES (9, 1002, '2026-04-06', 1, 25, 10);
INSERT INTO `doctor_schedule` VALUES (10, 1002, '2026-04-10', 0, 25, 15);
INSERT INTO `doctor_schedule` VALUES (11, 1002, '2026-04-13', 0, 25, 5);
INSERT INTO `doctor_schedule` VALUES (12, 1003, '2026-04-05', 0, 10, 10);
INSERT INTO `doctor_schedule` VALUES (13, 1003, '2026-04-08', 0, 10, 10);
INSERT INTO `doctor_schedule` VALUES (14, 1003, '2026-04-12', 0, 10, 9);
INSERT INTO `doctor_schedule` VALUES (15, 1003, '2026-04-17', 0, 10, 4);
INSERT INTO `doctor_schedule` VALUES (16, 1004, '2026-04-04', 1, 20, 5);
INSERT INTO `doctor_schedule` VALUES (17, 1004, '2026-04-06', 0, 20, 8);
INSERT INTO `doctor_schedule` VALUES (18, 1004, '2026-04-08', 1, 20, 2);
INSERT INTO `doctor_schedule` VALUES (19, 1004, '2026-04-11', 0, 20, 1);
INSERT INTO `doctor_schedule` VALUES (20, 1004, '2026-04-15', 0, 20, 0);
INSERT INTO `doctor_schedule` VALUES (21, 1005, '2026-04-05', 0, 15, 8);
INSERT INTO `doctor_schedule` VALUES (22, 1005, '2026-04-07', 1, 15, 5);
INSERT INTO `doctor_schedule` VALUES (23, 1005, '2026-04-10', 0, 15, 10);
INSERT INTO `doctor_schedule` VALUES (24, 1005, '2026-04-14', 1, 15, 2);
INSERT INTO `doctor_schedule` VALUES (25, 1006, '2026-04-04', 0, 20, 18);
INSERT INTO `doctor_schedule` VALUES (26, 1006, '2026-04-05', 0, 20, 20);
INSERT INTO `doctor_schedule` VALUES (27, 1006, '2026-04-08', 0, 20, 15);
INSERT INTO `doctor_schedule` VALUES (28, 1006, '2026-04-11', 1, 20, 6);
INSERT INTO `doctor_schedule` VALUES (29, 1006, '2026-04-16', 0, 20, 1);
INSERT INTO `doctor_schedule` VALUES (30, 1007, '2026-04-06', 0, 12, 12);
INSERT INTO `doctor_schedule` VALUES (31, 1007, '2026-04-09', 0, 12, 12);
INSERT INTO `doctor_schedule` VALUES (32, 1007, '2026-04-13', 0, 12, 8);
INSERT INTO `doctor_schedule` VALUES (33, 1007, '2026-04-17', 0, 12, 0);
INSERT INTO `doctor_schedule` VALUES (34, 1008, '2026-04-05', 1, 20, 10);
INSERT INTO `doctor_schedule` VALUES (35, 1008, '2026-04-07', 0, 20, 12);
INSERT INTO `doctor_schedule` VALUES (36, 1008, '2026-04-09', 0, 20, 8);
INSERT INTO `doctor_schedule` VALUES (37, 1008, '2026-04-12', 1, 20, 3);
INSERT INTO `doctor_schedule` VALUES (38, 1008, '2026-04-14', 0, 20, 0);
INSERT INTO `doctor_schedule` VALUES (39, 1009, '2026-04-04', 0, 20, 15);
INSERT INTO `doctor_schedule` VALUES (40, 1009, '2026-04-06', 1, 20, 12);
INSERT INTO `doctor_schedule` VALUES (41, 1009, '2026-04-08', 0, 20, 18);
INSERT INTO `doctor_schedule` VALUES (42, 1009, '2026-04-10', 0, 20, 9);
INSERT INTO `doctor_schedule` VALUES (43, 1009, '2026-04-15', 1, 20, 2);
INSERT INTO `doctor_schedule` VALUES (44, 1010, '2026-04-05', 0, 15, 10);
INSERT INTO `doctor_schedule` VALUES (45, 1010, '2026-04-05', 1, 15, 8);
INSERT INTO `doctor_schedule` VALUES (46, 1010, '2026-04-08', 0, 15, 12);
INSERT INTO `doctor_schedule` VALUES (47, 1010, '2026-04-11', 1, 15, 5);
INSERT INTO `doctor_schedule` VALUES (48, 1010, '2026-04-16', 0, 15, 1);
INSERT INTO `doctor_schedule` VALUES (49, 1010, '2026-04-17', 0, 10, 9);

-- 初始化后自动校正排班日期到未来（避免导库后全部“过期不可约”）
-- 规则：若当前最大排班日早于“明天”，则整体平移到至少明天
SET @target_future_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY);
SET @seed_max_work_date = (SELECT MAX(work_date) FROM doctor_schedule);
SET @seed_shift_days = (
  SELECT CASE
    WHEN @seed_max_work_date IS NULL THEN 0
    WHEN @seed_max_work_date >= @target_future_date THEN 0
    ELSE DATEDIFF(@target_future_date, @seed_max_work_date)
  END
);

UPDATE doctor_schedule
SET work_date = DATE_ADD(work_date, INTERVAL @seed_shift_days DAY)
WHERE @seed_shift_days > 0;

-- 初始化兜底：保证未来排班至少留有可预约号源
UPDATE doctor_schedule
SET max_capacity = 10
WHERE work_date >= CURDATE()
  AND (max_capacity IS NULL OR max_capacity <= 0);

UPDATE doctor_schedule
SET booked_count = LEAST(GREATEST(booked_count, 0), GREATEST(max_capacity - 1, 0))
WHERE work_date >= CURDATE()
  AND max_capacity > 0;

-- 依据固定班次模板补齐未来30天排班（避免测试中断档）
INSERT INTO doctor_schedule (doctor_id, work_date, time_slot, max_capacity, booked_count)
SELECT
  p.doctor_id,
  DATE_ADD(CURDATE(), INTERVAL d.n DAY) AS work_date,
  p.time_slot,
  p.default_capacity,
  0
FROM doctor_work_pattern p
JOIN (
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) d
WHERE p.active = 1
  AND p.weekday = WEEKDAY(DATE_ADD(CURDATE(), INTERVAL d.n DAY)) + 1
  AND NOT EXISTS (
    SELECT 1
    FROM doctor_schedule s
    WHERE s.doctor_id = p.doctor_id
      AND s.work_date = DATE_ADD(CURDATE(), INTERVAL d.n DAY)
      AND s.time_slot = p.time_slot
  );

-- ----------------------------
-- Table structure for emergency_record
-- ----------------------------
DROP TABLE IF EXISTS `emergency_record`;
CREATE TABLE `emergency_record`  (
  `help_id` bigint NOT NULL AUTO_INCREMENT COMMENT '求助唯一标识',
  `user_id` bigint NULL DEFAULT NULL COMMENT '求助老人ID',
  `help_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '求助发生时间',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '位置/地址信息',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '处理状态(0待处理,1家属接单,2社区接单,3已解决)',
  `handle_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理结果说明',
  PRIMARY KEY (`help_id`) USING BTREE,
  INDEX `fk_emergency_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_emergency_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '紧急求助记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of emergency_record
-- ----------------------------
INSERT INTO `emergency_record` VALUES (1, 1021, '2026-02-08 07:36:45', '小区3号楼2单元501室', 3, '老人头晕，家属已到家照顾，无大碍');
INSERT INTO `emergency_record` VALUES (2, 1024, '2026-02-19 09:12:08', '社区活动中心', 2, '血糖偏高，社区工作人员协助服药并联系家属');
INSERT INTO `emergency_record` VALUES (3, 1028, '2026-02-27 21:05:22', '小区6号楼家中客厅', 1, '胸闷不适，家属紧急处理后送往医院');
INSERT INTO `emergency_record` VALUES (4, 1032, '2026-03-03 10:28:40', '小区花园长椅', 3, '轻微摔倒，无外伤，社区已送回家并通知家属');
INSERT INTO `emergency_record` VALUES (5, 1035, '2026-03-10 14:47:12', '小区3号楼家中卧室', 2, '心率过快，社区上门监测后恢复正常');
INSERT INTO `emergency_record` VALUES (6, 1040, '2026-03-15 16:22:33', '小区门口便利店旁', 1, '低血糖头晕，家属及时赶到喂食糖果缓解');
INSERT INTO `emergency_record` VALUES (7, 1042, '2026-03-21 18:31:06', '小区8号楼家中厨房', 3, '忘记关火触发报警，社区上门处理，无危险');
INSERT INTO `emergency_record` VALUES (8, 1022, '2026-03-28 17:10:18', '小区健身区', 2, '运动后头晕，休息片刻恢复，已通知家属');
INSERT INTO `emergency_record` VALUES (9, 1038, '2026-04-06 06:58:52', '小区3号楼家中', 1, '血压骤升，家属陪同前往医院就诊');
INSERT INTO `emergency_record` VALUES (10, 1048, '2026-04-09 20:06:27', '小区楼道内', 3, '行走不慎滑倒，社区协助回家，无受伤');
INSERT INTO `emergency_record` VALUES (11, 1025, '2026-04-22 22:18:05', '小区6号楼家中', 0, '身体不适按求助键，等待家属响应');
INSERT INTO `emergency_record` VALUES (12, 1030, '2026-04-24 23:11:36', '定位坐标(30.530822, 114.345138)', 0, '夜间心慌求助，待处理');

-- ----------------------------
-- Table structure for family_bind
-- ----------------------------
DROP TABLE IF EXISTS `family_bind`;
CREATE TABLE `family_bind`  (
  `bind_id` bigint NOT NULL AUTO_INCREMENT COMMENT '绑定唯一标识',
  `elder_id` bigint NULL DEFAULT NULL COMMENT '老年人ID',
  `family_id` bigint NULL DEFAULT NULL COMMENT '家属ID',
  `relation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '亲属关系',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  PRIMARY KEY (`bind_id`) USING BTREE,
  INDEX `fk_bind_elder`(`elder_id` ASC) USING BTREE,
  INDEX `fk_bind_family`(`family_id` ASC) USING BTREE,
  CONSTRAINT `fk_bind_elder` FOREIGN KEY (`elder_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_bind_family` FOREIGN KEY (`family_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '家属绑定关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of family_bind
-- ----------------------------
INSERT INTO `family_bind` VALUES (1, 1021, 1049, '父子', '2025-09-05 10:12:16');
INSERT INTO `family_bind` VALUES (2, 1021, 1050, '父女', '2025-09-05 10:26:33');
INSERT INTO `family_bind` VALUES (3, 1022, 1051, '父子', '2025-09-12 09:48:55');
INSERT INTO `family_bind` VALUES (4, 1023, 1052, '母子', '2025-09-18 14:15:29');
INSERT INTO `family_bind` VALUES (5, 1023, 1053, '母女', '2025-09-18 14:27:03');
INSERT INTO `family_bind` VALUES (6, 1024, 1054, '父子', '2025-09-24 11:09:44');
INSERT INTO `family_bind` VALUES (7, 1025, 1055, '母女', '2025-09-30 16:21:18');
INSERT INTO `family_bind` VALUES (8, 1026, 1056, '父子', '2025-10-06 10:42:37');
INSERT INTO `family_bind` VALUES (9, 1027, 1057, '母女', '2025-10-10 15:08:52');
INSERT INTO `family_bind` VALUES (10, 1028, 1058, '父子', '2025-10-14 09:33:27');
INSERT INTO `family_bind` VALUES (11, 1029, 1059, '父子', '2025-10-19 13:16:40');
INSERT INTO `family_bind` VALUES (12, 1029, 1060, '父女', '2025-10-19 13:22:14');
INSERT INTO `family_bind` VALUES (13, 1030, 1061, '父子', '2025-10-25 11:37:48');
INSERT INTO `family_bind` VALUES (14, 1030, 1062, '父女', '2025-10-25 11:44:29');
INSERT INTO `family_bind` VALUES (15, 1031, 1063, '父子', '2025-11-01 09:58:03');
INSERT INTO `family_bind` VALUES (16, 1031, 1064, '父子', '2025-11-01 10:05:15');
INSERT INTO `family_bind` VALUES (17, 1032, 1065, '夫妻', '2025-11-06 15:20:06');
INSERT INTO `family_bind` VALUES (18, 1032, 1066, '母女', '2025-11-06 15:27:53');
INSERT INTO `family_bind` VALUES (19, 1033, 1067, '母女', '2025-11-12 10:41:22');
INSERT INTO `family_bind` VALUES (20, 1033, 1068, '父子', '2025-11-12 10:49:39');
INSERT INTO `family_bind` VALUES (21, 1034, 1069, '母子', '2025-11-18 14:10:57');
INSERT INTO `family_bind` VALUES (22, 1034, 1070, '母女', '2025-11-18 14:18:43');
INSERT INTO `family_bind` VALUES (23, 1035, 1071, '夫妻', '2025-11-24 09:27:34');
INSERT INTO `family_bind` VALUES (24, 1035, 1072, '父女', '2025-11-24 09:34:21');
INSERT INTO `family_bind` VALUES (25, 1036, 1073, '父子', '2025-11-29 11:15:48');
INSERT INTO `family_bind` VALUES (26, 1036, 1074, '父女', '2025-11-29 11:22:30');
INSERT INTO `family_bind` VALUES (27, 1037, 1075, '母子', '2025-12-05 16:09:13');
INSERT INTO `family_bind` VALUES (28, 1037, 1076, '母女', '2025-12-05 16:16:40');
INSERT INTO `family_bind` VALUES (29, 1038, 1077, '父子', '2025-12-11 10:52:08');
INSERT INTO `family_bind` VALUES (30, 1038, 1078, '父子', '2025-12-11 11:00:26');
INSERT INTO `family_bind` VALUES (31, 1039, 1079, '夫妻', '2025-12-17 09:45:14');
INSERT INTO `family_bind` VALUES (32, 1039, 1080, '父女', '2025-12-17 09:53:47');
INSERT INTO `family_bind` VALUES (33, 1040, 1081, '父子', '2025-12-23 13:22:31');
INSERT INTO `family_bind` VALUES (34, 1040, 1082, '父女', '2025-12-23 13:29:20');
INSERT INTO `family_bind` VALUES (35, 1041, 1083, '父子', '2025-12-29 10:14:58');
INSERT INTO `family_bind` VALUES (36, 1041, 1084, '父女', '2025-12-29 10:22:43');
INSERT INTO `family_bind` VALUES (37, 1042, 1085, '夫妻', '2026-01-04 15:05:36');
INSERT INTO `family_bind` VALUES (38, 1042, 1086, '父女', '2026-01-04 15:11:59');
INSERT INTO `family_bind` VALUES (39, 1043, 1087, '父子', '2026-01-09 11:38:17');
INSERT INTO `family_bind` VALUES (40, 1043, 1088, '母女', '2026-01-09 11:46:28');
INSERT INTO `family_bind` VALUES (41, 1044, 1089, '父子', '2026-01-15 14:43:05');
INSERT INTO `family_bind` VALUES (42, 1044, 1090, '父子', '2026-01-15 14:50:54');
INSERT INTO `family_bind` VALUES (43, 1045, 1091, '母女', '2026-01-21 09:16:12');
INSERT INTO `family_bind` VALUES (44, 1045, 1092, '母子', '2026-01-21 09:23:43');
INSERT INTO `family_bind` VALUES (45, 1046, 1093, '父女', '2026-01-27 16:02:26');
INSERT INTO `family_bind` VALUES (46, 1046, 1094, '父子', '2026-01-27 16:10:57');
INSERT INTO `family_bind` VALUES (47, 1047, 1095, '夫妻', '2026-02-03 10:27:38');
INSERT INTO `family_bind` VALUES (48, 1047, 1096, '父子', '2026-02-03 10:35:12');
INSERT INTO `family_bind` VALUES (49, 1048, 1097, '母女', '2026-02-08 13:58:04');
INSERT INTO `family_bind` VALUES (50, 1048, 1098, '母子', '2026-02-08 14:06:35');

-- ----------------------------
-- Table structure for health_records
-- ----------------------------
DROP TABLE IF EXISTS `health_records`;
CREATE TABLE `health_records`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录唯一标识',
  `user_id` bigint NULL DEFAULT NULL COMMENT '所属用户ID(老人)',
  `blood_pressure_high` float(4,1) NULL DEFAULT NULL COMMENT '收缩压(高压)',
  `blood_pressure_low` float(4,1) NULL DEFAULT NULL COMMENT '舒张压(低压)',
  `heart_rate` int NULL DEFAULT NULL COMMENT '心率',
  `blood_sugar` float(4,1) NULL DEFAULT NULL COMMENT '血糖',
  `medication_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用药信息(用于语音播报)',
  `record_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `fk_health_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_health_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康档案表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_records
-- ----------------------------
INSERT INTO `health_records` VALUES (1, 1021, 135.0, 85.0, 72, 5.5, '阿司匹林，每日一次', '2026-04-02 07:42:15');
INSERT INTO `health_records` VALUES (2, 1022, 140.5, 90.0, 78, 6.2, '硝苯地平，每日一次', '2026-04-03 08:12:45');
INSERT INTO `health_records` VALUES (3, 1023, 125.0, 80.0, 65, 5.1, '目前无需用药', '2026-04-04 07:35:52');
INSERT INTO `health_records` VALUES (4, 1024, 150.0, 95.0, 85, 7.8, '二甲双胍，每日三次', '2026-04-02 20:25:44');
INSERT INTO `health_records` VALUES (5, 1025, 130.0, 85.0, 70, 5.8, '阿托伐他汀，每晚一次', '2026-04-01 07:50:13');
INSERT INTO `health_records` VALUES (6, 1026, 118.0, 75.0, 68, 4.9, '钙片，每日一次', '2026-04-03 08:40:31');
INSERT INTO `health_records` VALUES (7, 1027, 145.0, 88.0, 82, 6.5, '阿司匹林，每日一次', '2026-04-04 19:10:52');
INSERT INTO `health_records` VALUES (8, 1028, 138.0, 82.0, 75, 5.4, '速效救心丸，必要时服', '2026-04-05 08:02:18');
INSERT INTO `health_records` VALUES (9, 1021, 132.0, 83.0, 70, 5.3, '阿司匹林，每日一次', '2026-04-09 20:15:28');
INSERT INTO `health_records` VALUES (10, 1022, 138.0, 88.0, 76, 6.0, '硝苯地平，每日一次', '2026-04-10 19:36:11');
INSERT INTO `health_records` VALUES (11, 1024, 142.0, 90.0, 80, 7.1, '二甲双胍，每日三次，增加胰岛素', '2026-04-08 08:05:22');
INSERT INTO `health_records` VALUES (12, 1029, 138.0, 85.0, 72, 5.8, '硝苯地平，每日一次', '2026-04-06 07:48:26');
INSERT INTO `health_records` VALUES (13, 1029, 132.0, 82.0, 70, 5.6, '硝苯地平，每日一次，血压控制良好', '2026-04-13 20:06:35');
INSERT INTO `health_records` VALUES (14, 1030, 150.0, 95.0, 80, 7.5, '二甲双胍，每日二次；厄贝沙坦', '2026-04-06 19:40:08');
INSERT INTO `health_records` VALUES (15, 1030, 142.0, 88.0, 75, 6.9, '二甲双胍，每日二次；厄贝沙坦', '2026-04-15 08:09:14');
INSERT INTO `health_records` VALUES (16, 1031, 125.0, 75.0, 68, 4.8, '目前无需特殊用药，建议保持运动', '2026-04-07 08:21:47');
INSERT INTO `health_records` VALUES (17, 1031, 128.0, 78.0, 70, 5.0, '目前无需特殊用药，注意保暖', '2026-04-16 19:14:55');
INSERT INTO `health_records` VALUES (18, 1032, 145.0, 88.0, 82, 6.1, '氨氯地平，每日一次', '2026-04-07 19:56:03');
INSERT INTO `health_records` VALUES (19, 1032, 136.0, 82.0, 78, 5.9, '氨氯地平，每日一次', '2026-04-17 08:27:49');
INSERT INTO `health_records` VALUES (20, 1033, 130.0, 80.0, 72, 8.8, '格列齐特，每日一次，严格控糖', '2026-04-08 08:16:37');
INSERT INTO `health_records` VALUES (21, 1033, 128.0, 78.0, 70, 7.5, '格列齐特，每日一次，血糖有所下降', '2026-04-18 19:33:28');
INSERT INTO `health_records` VALUES (22, 1034, 118.0, 70.0, 65, 5.2, '骨化三醇及钙片，每日一次', '2026-04-08 07:59:41');
INSERT INTO `health_records` VALUES (23, 1034, 120.0, 72.0, 68, 5.1, '骨化三醇及钙片，每日一次', '2026-04-19 08:41:16');
INSERT INTO `health_records` VALUES (24, 1035, 145.0, 90.0, 92, 5.5, '倍他乐克，每日一次，控制心率', '2026-04-09 19:22:05');
INSERT INTO `health_records` VALUES (25, 1035, 138.0, 85.0, 82, 5.4, '倍他乐克，每日一次，心率平稳', '2026-04-20 20:18:42');
INSERT INTO `health_records` VALUES (26, 1036, 135.0, 85.0, 75, 6.8, '阿托伐他汀，每晚一次', '2026-04-09 08:07:33');
INSERT INTO `health_records` VALUES (27, 1036, 130.0, 82.0, 72, 6.2, '阿托伐他汀，每晚一次', '2026-04-20 19:25:50');
INSERT INTO `health_records` VALUES (28, 1037, 122.0, 78.0, 70, 4.9, '指标正常，建议适量散步', '2026-04-10 07:44:19');
INSERT INTO `health_records` VALUES (29, 1037, 125.0, 80.0, 72, 5.1, '指标正常，继续保持', '2026-04-21 08:30:24');
INSERT INTO `health_records` VALUES (30, 1038, 165.0, 105.0, 88, 7.2, '厄贝沙坦氢氯噻嗪，每日一次', '2026-04-10 20:11:57');
INSERT INTO `health_records` VALUES (31, 1038, 148.0, 95.0, 82, 6.8, '厄贝沙坦氢氯噻嗪，血压开始回落', '2026-04-21 08:13:09');
INSERT INTO `health_records` VALUES (32, 1039, 130.0, 80.0, 75, 5.4, '阿司匹林、速效救心丸随身携带', '2026-04-11 08:38:46');
INSERT INTO `health_records` VALUES (33, 1039, 132.0, 82.0, 72, 5.3, '按时服药，近期无胸闷症状', '2026-04-22 19:57:31');
INSERT INTO `health_records` VALUES (34, 1040, 140.0, 88.0, 80, 10.5, '胰岛素注射，每日二次', '2026-04-11 19:48:14');
INSERT INTO `health_records` VALUES (35, 1040, 138.0, 85.0, 78, 8.2, '胰岛素注射，空腹血糖改善', '2026-04-22 08:20:58');
INSERT INTO `health_records` VALUES (36, 1041, 138.0, 82.0, 68, 5.0, '非洛地平，每日一次', '2026-04-12 08:11:26');
INSERT INTO `health_records` VALUES (37, 1041, 130.0, 78.0, 66, 4.9, '非洛地平，每日一次', '2026-04-23 19:05:47');
INSERT INTO `health_records` VALUES (38, 1042, 145.0, 92.0, 95, 6.5, '美托洛尔，每日二次', '2026-04-12 20:03:35');
INSERT INTO `health_records` VALUES (39, 1042, 140.0, 88.0, 82, 6.2, '美托洛尔，心率恢复正常范围', '2026-04-23 08:24:12');
INSERT INTO `health_records` VALUES (40, 1043, 135.0, 82.0, 76, 5.8, '辅酶Q10，阿司匹林', '2026-04-13 07:57:04');
INSERT INTO `health_records` VALUES (41, 1043, 132.0, 80.0, 74, 5.5, '辅酶Q10，阿司匹林，维持原方案', '2026-04-24 19:39:26');
INSERT INTO `health_records` VALUES (42, 1044, 155.0, 95.0, 88, 9.2, '阿卡波糖 餐时服用；硝苯地平', '2026-04-13 19:29:53');
INSERT INTO `health_records` VALUES (43, 1044, 145.0, 88.0, 84, 7.8, '按原方案服药，各项指标改善', '2026-04-24 08:35:17');
INSERT INTO `health_records` VALUES (44, 1045, 125.0, 78.0, 70, 5.2, '碳酸钙D3，每日一次', '2026-04-14 08:06:42');
INSERT INTO `health_records` VALUES (45, 1045, 122.0, 75.0, 68, 5.0, '碳酸钙D3，每日一次', '2026-04-25 19:11:05');
INSERT INTO `health_records` VALUES (46, 1046, 138.0, 85.0, 75, 6.0, '辛伐他汀，每晚一次，清淡饮食', '2026-04-14 19:52:33');
INSERT INTO `health_records` VALUES (47, 1046, 135.0, 82.0, 72, 5.8, '辛伐他汀，坚持低脂饮食有效', '2026-04-25 08:17:48');
INSERT INTO `health_records` VALUES (48, 1047, 142.0, 88.0, 82, 7.5, '二甲双胍，每日一次', '2026-04-15 08:28:56');
INSERT INTO `health_records` VALUES (49, 1047, 138.0, 85.0, 80, 6.6, '二甲双胍，血糖控制在合格线', '2026-04-25 19:26:14');
INSERT INTO `health_records` VALUES (50, 1048, 130.0, 80.0, 65, 5.5, '稳心颗粒，每日三次', '2026-04-15 19:15:21');
INSERT INTO `health_records` VALUES (51, 1048, 128.0, 78.0, 68, 5.4, '稳心颗粒，症状缓解', '2026-04-26 08:04:39');
INSERT INTO `health_records` VALUES (52, 1021, 134.0, 82.0, 71, 5.3, '阿司匹林，每日一次，复查情况稳定', '2026-04-17 08:10:06');
INSERT INTO `health_records` VALUES (53, 1021, 130.0, 80.0, 70, 5.2, '阿司匹林，每日一次，血压极佳', '2026-04-25 19:48:22');
INSERT INTO `health_records` VALUES (54, 1022, 136.0, 85.0, 75, 5.9, '硝苯地平，每日一次，减量测试', '2026-04-18 07:58:30');
INSERT INTO `health_records` VALUES (55, 1022, 135.0, 84.0, 74, 5.8, '硝苯地平，维持减量后状态良好', '2026-04-26 20:04:40');
INSERT INTO `health_records` VALUES (56, 1023, 124.0, 78.0, 64, 5.0, '目前无需用药，日常随访', '2026-04-12 08:22:35');
INSERT INTO `health_records` VALUES (57, 1023, 122.0, 75.0, 62, 4.9, '血压心率完美，建议继续跳广场舞', '2026-04-20 19:18:09');
INSERT INTO `health_records` VALUES (58, 1023, 120.0, 72.0, 60, 4.8, '常规体检，未见异常', '2026-04-26 07:46:54');
INSERT INTO `health_records` VALUES (59, 1024, 140.0, 88.0, 78, 6.8, '二甲双胍，增加胰岛素后血糖下降', '2026-04-15 19:42:16');
INSERT INTO `health_records` VALUES (60, 1024, 138.0, 85.0, 75, 6.5, '二甲双胍，胰岛素减量', '2026-04-21 08:18:07');
INSERT INTO `health_records` VALUES (61, 1024, 135.0, 82.0, 72, 6.2, '二甲双胍，每日三次，状态恢复不错', '2026-04-26 19:31:55');
INSERT INTO `health_records` VALUES (62, 1025, 128.0, 82.0, 68, 5.6, '阿托伐他汀，每晚一次', '2026-04-16 19:27:39');
INSERT INTO `health_records` VALUES (63, 1025, 125.0, 80.0, 66, 5.4, '阿托伐他汀，血脂复查已达标', '2026-04-22 08:33:21');
INSERT INTO `health_records` VALUES (64, 1025, 126.0, 81.0, 67, 5.5, '阿托伐他汀，隔日一次维持', '2026-04-26 20:12:08');
INSERT INTO `health_records` VALUES (65, 1026, 115.0, 72.0, 65, 4.8, '钙片，每日一次，骨密度复查', '2026-04-16 07:55:44');
INSERT INTO `health_records` VALUES (66, 1026, 118.0, 74.0, 66, 4.9, '钙片，每日一次，关节疼痛缓解', '2026-04-23 19:20:36');
INSERT INTO `health_records` VALUES (67, 1026, 120.0, 75.0, 68, 5.0, '停用钙片，改为食补', '2026-04-26 08:26:11');
INSERT INTO `health_records` VALUES (68, 1027, 142.0, 85.0, 80, 6.3, '阿司匹林，每日一次，微调剂量', '2026-04-17 08:14:20');
INSERT INTO `health_records` VALUES (69, 1027, 140.0, 82.0, 78, 6.1, '阿司匹林，每日一次，情况平稳', '2026-04-24 19:45:12');
INSERT INTO `health_records` VALUES (70, 1028, 135.0, 80.0, 72, 5.2, '速效救心丸，必要时服，近期未发作', '2026-04-18 19:28:27');
INSERT INTO `health_records` VALUES (71, 1028, 132.0, 78.0, 70, 5.0, '常规心电图复查正常，继续观察', '2026-04-26 08:11:43');

-- 将健康记录时间统一压缩到“按导入当天动态计算”的最近14天（含今天）
-- 说明：仅重排 record_time，不改任何健康指标值
UPDATE `health_records`
SET `record_time` = TIMESTAMP(
  DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 13 DAY), INTERVAL ((`record_id` - 1) % 14) DAY),
  MAKETIME(
    CASE WHEN `record_id` % 3 = 0 THEN 7 WHEN `record_id` % 3 = 1 THEN 8 ELSE 19 END,
    (`record_id` * 7) % 60,
    (`record_id` * 11) % 60
  )
);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码(加密)',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `role` tinyint(1) NULL DEFAULT NULL COMMENT '角色(0老人,1家属,2医生,3社区)',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1104 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1001, 'dr_li', '123456', '李医生', 40, 2, '13900002222');
INSERT INTO `user` VALUES (1002, 'dr_zhang', '123456', '张医生', 45, 2, '13900004444');
INSERT INTO `user` VALUES (1003, 'dr_wang', '123456', '王主任', 50, 2, '13900007777');
INSERT INTO `user` VALUES (1004, 'dr_zhao', '123456', '赵医师', 38, 2, '13900000000');
INSERT INTO `user` VALUES (1005, 'dr_chen', '123456', '陈医生', 42, 2, '13900001098');
INSERT INTO `user` VALUES (1006, 'dr_liu', '123456', '刘医生', 38, 2, '13900001099');
INSERT INTO `user` VALUES (1007, 'dr_yang', '123456', '杨主任', 55, 2, '13900001100');
INSERT INTO `user` VALUES (1008, 'dr_huang', '123456', '黄医师', 32, 2, '13900001101');
INSERT INTO `user` VALUES (1009, 'dr_wu', '123456', '吴医生', 46, 2, '13900001102');
INSERT INTO `user` VALUES (1010, 'dr_xu', '123456', '许医生', 35, 2, '13900001103');
INSERT INTO `user` VALUES (1011, 'cw_zhang', '123456', '张社工', 28, 3, '13700001013');
INSERT INTO `user` VALUES (1012, 'cw_li', '123456', '李网格员', 35, 3, '13700001014');
INSERT INTO `user` VALUES (1013, 'cw_wang', '123456', '王干事', 42, 3, '13700001015');
INSERT INTO `user` VALUES (1014, 'cw_zhao', '123456', '赵主任', 50, 3, '13700001016');
INSERT INTO `user` VALUES (1015, 'cw_chen', '123456', '陈专员', 25, 3, '13700001017');
INSERT INTO `user` VALUES (1016, 'cw_liu', '123456', '刘社工', 30, 3, '13700001018');
INSERT INTO `user` VALUES (1017, 'cw_sun', '123456', '孙助理', 26, 3, '13700001019');
INSERT INTO `user` VALUES (1018, 'cw_wu', '123456', '吴站长', 45, 3, '13700001020');
INSERT INTO `user` VALUES (1019, 'cw_zheng', '123456', '郑网格员', 33, 3, '13700001021');
INSERT INTO `user` VALUES (1020, 'cw_zhou', '123456', '周干事', 38, 3, '13700001022');
INSERT INTO `user` VALUES (1021, 'laowang', '123456', '王大爷', 75, 0, '13800001111');
INSERT INTO `user` VALUES (1022, 'zhaosi', '123456', '赵大爷', 68, 0, '13800003333');
INSERT INTO `user` VALUES (1023, 'liudama', '123456', '刘大妈', 72, 0, '13800005555');
INSERT INTO `user` VALUES (1024, 'chenbo', '123456', '陈伯', 80, 0, '13800006666');
INSERT INTO `user` VALUES (1025, 'sunayi', '123456', '孙阿姨', 65, 0, '13800008888');
INSERT INTO `user` VALUES (1026, 'zhoudaye', '123456', '周大爷', 78, 0, '13800009999');
INSERT INTO `user` VALUES (1027, 'wulaotai', '123456', '吴老太', 85, 0, '13800001011');
INSERT INTO `user` VALUES (1028, 'zhengge', '123456', '郑大爷', 70, 0, '13800001012');
INSERT INTO `user` VALUES (1029, 'qiandama', '123456', '钱大妈', 65, 0, '13800001033');
INSERT INTO `user` VALUES (1030, 'sundaye', '123456', '孙大爷', 72, 0, '13800001034');
INSERT INTO `user` VALUES (1031, 'lilaohan', '123456', '李老汉', 80, 0, '13800001035');
INSERT INTO `user` VALUES (1032, 'zhounainai', '123456', '周奶奶', 78, 0, '13800001036');
INSERT INTO `user` VALUES (1033, 'wubobo', '123456', '吴伯伯', 68, 0, '13800001037');
INSERT INTO `user` VALUES (1034, 'zhengbai', '123456', '郑阿姨', 62, 0, '13800001038');
INSERT INTO `user` VALUES (1035, 'wanglaotai', '123456', '王老太', 85, 0, '13800001039');
INSERT INTO `user` VALUES (1036, 'fengdaye', '123456', '冯大爷', 74, 0, '13800001040');
INSERT INTO `user` VALUES (1037, 'chendama', '123456', '陈大妈', 66, 0, '13800001041');
INSERT INTO `user` VALUES (1038, 'chudaye', '123456', '褚大爷', 79, 0, '13800001042');
INSERT INTO `user` VALUES (1039, 'weilaotou', '123456', '卫老头', 82, 0, '13800001043');
INSERT INTO `user` VALUES (1040, 'jiangayi', '123456', '蒋阿姨', 64, 0, '13800001044');
INSERT INTO `user` VALUES (1041, 'shendama', '123456', '沈大妈', 70, 0, '13800001045');
INSERT INTO `user` VALUES (1042, 'hanbobo', '123456', '韩伯伯', 75, 0, '13800001046');
INSERT INTO `user` VALUES (1043, 'yangnainai', '123456', '杨奶奶', 88, 0, '13800001047');
INSERT INTO `user` VALUES (1044, 'zhudaye', '123456', '朱大爷', 77, 0, '13800001048');
INSERT INTO `user` VALUES (1045, 'qinlaotai', '123456', '秦老太', 83, 0, '13800001049');
INSERT INTO `user` VALUES (1046, 'youdaye', '123456', '尤大爷', 71, 0, '13800001050');
INSERT INTO `user` VALUES (1047, 'xuayi', '123456', '许阿姨', 69, 0, '13800001051');
INSERT INTO `user` VALUES (1048, 'helaohan', '123456', '何老汉', 86, 0, '13800001052');
INSERT INTO `user` VALUES (1049, 'fm_wang_son', '123456', '王建国', 50, 1, '13500001023');
INSERT INTO `user` VALUES (1050, 'fm_wang_dau', '123456', '王丽丽', 45, 1, '13500001024');
INSERT INTO `user` VALUES (1051, 'fm_zhao_son', '123456', '赵强', 42, 1, '13500001025');
INSERT INTO `user` VALUES (1052, 'fm_liu_son', '123456', '李志刚', 48, 1, '13500001026');
INSERT INTO `user` VALUES (1053, 'fm_liu_dau', '123456', '李梅', 43, 1, '13500001027');
INSERT INTO `user` VALUES (1054, 'fm_chen_son', '123456', '陈斌', 55, 1, '13500001028');
INSERT INTO `user` VALUES (1055, 'fm_sun_dau', '123456', '周敏', 40, 1, '13500001029');
INSERT INTO `user` VALUES (1056, 'fm_zhou_son', '123456', '周杰', 52, 1, '13500001030');
INSERT INTO `user` VALUES (1057, 'fm_wu_dau', '123456', '吴小燕', 60, 1, '13500001031');
INSERT INTO `user` VALUES (1058, 'fm_zheng_son', '123456', '郑浩', 46, 1, '13500001032');
INSERT INTO `user` VALUES (1059, 'fm_zhao_g', '123456', '赵刚', 40, 1, '13600001053');
INSERT INTO `user` VALUES (1060, 'fm_zhao_x', '123456', '赵雪', 38, 1, '13600001054');
INSERT INTO `user` VALUES (1061, 'fm_sun_w', '123456', '孙伟', 45, 1, '13600001055');
INSERT INTO `user` VALUES (1062, 'fm_sun_q', '123456', '孙琴', 42, 1, '13600001056');
INSERT INTO `user` VALUES (1063, 'fm_li_jian', '123456', '李建', 55, 1, '13600001057');
INSERT INTO `user` VALUES (1064, 'fm_li_qiang', '123456', '李强', 50, 1, '13600001058');
INSERT INTO `user` VALUES (1065, 'fm_qian_y', '123456', '钱勇', 52, 1, '13600001059');
INSERT INTO `user` VALUES (1066, 'fm_qian_f', '123456', '钱芳', 48, 1, '13600001060');
INSERT INTO `user` VALUES (1067, 'fm_wu_j', '123456', '吴静', 40, 1, '13600001061');
INSERT INTO `user` VALUES (1068, 'fm_lin_t', '123456', '林涛', 42, 1, '13600001062');
INSERT INTO `user` VALUES (1069, 'fm_wang_y', '123456', '王洋', 35, 1, '13600001063');
INSERT INTO `user` VALUES (1070, 'fm_li_na', '123456', '李娜', 34, 1, '13600001064');
INSERT INTO `user` VALUES (1071, 'fm_zhang_gq', '123456', '张国庆', 60, 1, '13600001065');
INSERT INTO `user` VALUES (1072, 'fm_zhang_xy', '123456', '张秀英', 58, 1, '13500001066');
INSERT INTO `user` VALUES (1073, 'fm_feng_j', '123456', '冯军', 48, 1, '13600001067');
INSERT INTO `user` VALUES (1074, 'fm_feng_l', '123456', '冯兰', 45, 1, '13600001068');
INSERT INTO `user` VALUES (1075, 'fm_liu_b', '123456', '刘波', 42, 1, '13600001069');
INSERT INTO `user` VALUES (1076, 'fm_liu_y', '123456', '刘燕', 39, 1, '13600001070');
INSERT INTO `user` VALUES (1077, 'fm_chu_h', '123456', '褚辉', 50, 1, '13600001071');
INSERT INTO `user` VALUES (1078, 'fm_chu_m', '123456', '褚明', 46, 1, '13600001072');
INSERT INTO `user` VALUES (1079, 'fm_wei_p', '123456', '卫平', 55, 1, '13600001073');
INSERT INTO `user` VALUES (1080, 'fm_wei_h', '123456', '卫红', 52, 1, '13600001074');
INSERT INTO `user` VALUES (1081, 'fm_song_f', '123456', '宋飞', 38, 1, '13600001075');
INSERT INTO `user` VALUES (1082, 'fm_song_j', '123456', '宋佳', 35, 1, '13600001076');
INSERT INTO `user` VALUES (1083, 'fm_gao_l', '123456', '高雷', 46, 1, '13600001077');
INSERT INTO `user` VALUES (1084, 'fm_gao_p', '123456', '高萍', 42, 1, '13600001078');
INSERT INTO `user` VALUES (1085, 'fm_han_f', '123456', '韩峰', 49, 1, '13600001079');
INSERT INTO `user` VALUES (1086, 'fm_han_x', '123456', '韩雪', 45, 1, '13600001080');
INSERT INTO `user` VALUES (1087, 'fm_chen_zm', '123456', '陈志明', 62, 1, '13600001081');
INSERT INTO `user` VALUES (1088, 'fm_chen_yl', '123456', '陈玉兰', 59, 1, '13600001082');
INSERT INTO `user` VALUES (1089, 'fm_zhu_b', '123456', '朱斌', 48, 1, '13600001083');
INSERT INTO `user` VALUES (1090, 'fm_zhu_k', '123456', '朱凯', 45, 1, '13600001084');
INSERT INTO `user` VALUES (1091, 'fm_zhu_l', '123456', '朱丽', 42, 1, '13600001085');
INSERT INTO `user` VALUES (1092, 'fm_li_tie', '123456', '李铁', 58, 1, '13600001086');
INSERT INTO `user` VALUES (1093, 'fm_li_juan', '123456', '李娟', 55, 1, '13600001087');
INSERT INTO `user` VALUES (1094, 'fm_li_qin', '123456', '李琴', 50, 1, '13600001088');
INSERT INTO `user` VALUES (1095, 'fm_you_jh', '123456', '尤建华', 45, 1, '13600001089');
INSERT INTO `user` VALUES (1096, 'fm_you_jg', '123456', '尤建国', 43, 1, '13600001090');
INSERT INTO `user` VALUES (1097, 'fm_you_xf', '123456', '尤小芳', 40, 1, '13600001091');
INSERT INTO `user` VALUES (1098, 'fm_wang_lei', '123456', '王磊', 42, 1, '13600001092');
INSERT INTO `user` VALUES (1099, 'fm_wang_ting', '123456', '王婷', 39, 1, '13600001093');
INSERT INTO `user` VALUES (1100, 'fm_wang_yuan', '123456', '王媛', 35, 1, '13600001094');
INSERT INTO `user` VALUES (1101, 'fm_he_zh', '123456', '何中华', 60, 1, '13600001095');
INSERT INTO `user` VALUES (1102, 'fm_he_zx', '123456', '何中兴', 58, 1, '13600001096');
INSERT INTO `user` VALUES (1103, 'fm_he_zm', '123456', '何中梅', 55, 1, '13600001097');

-- ----------------------------
-- Table structure for building_geo_zone
-- ----------------------------
DROP TABLE IF EXISTS `building_geo_zone`;
CREATE TABLE `building_geo_zone` (
  `zone_id` bigint NOT NULL AUTO_INCREMENT COMMENT '区域ID',
  `building_no` int NOT NULL COMMENT '楼栋号',
  `min_latitude` double NOT NULL COMMENT '最小纬度',
  `max_latitude` double NOT NULL COMMENT '最大纬度',
  `min_longitude` double NOT NULL COMMENT '最小经度',
  `max_longitude` double NOT NULL COMMENT '最大经度',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启用状态(1启用,0停用)',
  `note` varchar(128) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`zone_id`) USING BTREE,
  UNIQUE KEY `uk_building_no` (`building_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼栋坐标映射表';

-- ----------------------------
-- Records of building_geo_zone
-- ----------------------------
INSERT INTO `building_geo_zone` (`building_no`,`min_latitude`,`max_latitude`,`min_longitude`,`max_longitude`,`active`,`note`) VALUES
(1, 30.530700, 30.530940, 114.344980, 114.345180, 1, '演示网格1栋'),
(2, 30.530700, 30.530940, 114.345180, 114.345380, 1, '演示网格2栋'),
(3, 30.530700, 30.530940, 114.345380, 114.345580, 1, '演示网格3栋'),
(4, 30.530700, 30.530940, 114.345580, 114.345780, 1, '演示网格4栋'),
(5, 30.530460, 30.530700, 114.344980, 114.345180, 1, '演示网格5栋'),
(6, 30.530460, 30.530700, 114.345180, 114.345380, 1, '演示网格6栋'),
(7, 30.530460, 30.530700, 114.345380, 114.345580, 1, '演示网格7栋'),
(8, 30.530460, 30.530700, 114.345580, 114.345780, 1, '演示网格8栋'),
(9, 30.530220, 30.530460, 114.344980, 114.345180, 1, '演示网格9栋'),
(10, 30.530220, 30.530460, 114.345180, 114.345380, 1, '演示网格10栋'),
(11, 30.530220, 30.530460, 114.345380, 114.345580, 1, '演示网格11栋'),
(12, 30.530220, 30.530460, 114.345580, 114.345780, 1, '演示网格12栋');

-- ----------------------------
-- Table structure for diagnosis_record
-- ----------------------------
DROP TABLE IF EXISTS `diagnosis_record`;
CREATE TABLE `diagnosis_record` (
  `diagnosis_id` bigint NOT NULL AUTO_INCREMENT COMMENT '诊断记录ID',
  `user_id` bigint NOT NULL COMMENT '老人ID',
  `doctor_id` bigint DEFAULT NULL COMMENT '诊断医生ID',
  `diagnosis_type` varchar(32) NOT NULL COMMENT '诊断类型(高血压/低血压/糖尿病/心率异常)',
  `note` varchar(255) DEFAULT NULL COMMENT '诊断备注',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效(1有效,0失效)',
  `diagnosis_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '诊断时间',
  PRIMARY KEY (`diagnosis_id`) USING BTREE,
  INDEX `idx_diag_user_time` (`user_id`, `diagnosis_time`) USING BTREE,
  INDEX `idx_diag_doctor` (`doctor_id`) USING BTREE,
  CONSTRAINT `fk_diag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_diag_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='老人诊断记录表';

-- ----------------------------
-- Records of diagnosis_record
-- ----------------------------
INSERT INTO `diagnosis_record` VALUES
(1, 1021, 1001, '高血压', '血压控制中，持续随访', 1, '2026-04-10 09:20:00'),
(2, 1022, 1002, '高血压', '建议低盐饮食', 1, '2026-04-11 10:10:00'),
(3, 1023, 1003, '心率异常', '心率偏快，建议复查心电图', 1, '2026-04-12 15:00:00'),
(4, 1024, 1001, '糖尿病', '空腹血糖偏高，建议控糖', 1, '2026-04-13 08:40:00'),
(5, 1038, 1002, '低血压', '晨起低压偏低，注意补水', 1, '2026-04-14 14:00:00');

SET FOREIGN_KEY_CHECKS = 1;