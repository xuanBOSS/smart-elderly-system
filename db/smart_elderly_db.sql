/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 90600 (9.6.0)
 Source Host           : localhost:3306
 Source Schema         : smart_elderly_db

 Target Server Type    : MySQL
 Target Server Version : 90600 (9.6.0)
 File Encoding         : 65001

 Date: 03/04/2026 21:36:55
*/

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
INSERT INTO `family_bind` VALUES (1, 1021, 1049, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (2, 1021, 1050, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (3, 1022, 1051, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (4, 1023, 1052, '母子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (5, 1023, 1053, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (6, 1024, 1054, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (7, 1025, 1055, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (8, 1026, 1056, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (9, 1027, 1057, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (10, 1028, 1058, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (11, 1029, 1059, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (12, 1029, 1060, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (13, 1030, 1061, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (14, 1030, 1062, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (15, 1031, 1063, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (16, 1031, 1064, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (17, 1032, 1065, '夫妻', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (18, 1032, 1066, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (19, 1033, 1067, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (20, 1033, 1068, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (21, 1034, 1069, '母子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (22, 1034, 1070, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (23, 1035, 1071, '夫妻', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (24, 1035, 1072, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (25, 1036, 1073, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (26, 1036, 1074, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (27, 1037, 1075, '母子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (28, 1037, 1076, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (29, 1038, 1077, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (30, 1038, 1078, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (31, 1039, 1079, '夫妻', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (32, 1039, 1080, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (33, 1040, 1081, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (34, 1040, 1082, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (35, 1041, 1083, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (36, 1041, 1084, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (37, 1042, 1085, '夫妻', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (38, 1042, 1086, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (39, 1043, 1087, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (40, 1043, 1088, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (41, 1044, 1089, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (42, 1044, 1090, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (43, 1045, 1091, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (44, 1045, 1092, '母子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (45, 1046, 1093, '父女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (46, 1046, 1094, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (47, 1047, 1095, '夫妻', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (48, 1047, 1096, '父子', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (49, 1048, 1097, '母女', '2026-04-03 21:34:03');
INSERT INTO `family_bind` VALUES (50, 1048, 1098, '母子', '2026-04-03 21:34:03');

-- ----------------------------
-- Table structure for health_records
-- ----------------------------
DROP TABLE IF EXISTS `health_records`;
CREATE TABLE `health_records`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录唯一标识',
  `user_id` bigint NULL DEFAULT NULL COMMENT '所属用户ID(老人)',
  `blood_pressure_high` float(4, 1) NULL DEFAULT NULL COMMENT '收缩压(高压)',
  `blood_pressure_low` float(4, 1) NULL DEFAULT NULL COMMENT '舒张压(低压)',
  `heart_rate` int NULL DEFAULT NULL COMMENT '心率',
  `blood_sugar` float(4, 1) NULL DEFAULT NULL COMMENT '血糖',
  `medication_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用药信息(用于语音播报)',
  `record_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `fk_health_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_health_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康档案表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_records
-- ----------------------------
INSERT INTO `health_records` VALUES (1, 1021, 135.0, 85.0, 72, 5.5, '阿司匹林，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (2, 1022, 140.5, 90.0, 78, 6.2, '硝苯地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (3, 1023, 125.0, 80.0, 65, 5.1, '目前无需用药', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (4, 1024, 150.0, 95.0, 85, 7.8, '二甲双胍，每日三次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (5, 1025, 130.0, 85.0, 70, 5.8, '阿托伐他汀，每晚一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (6, 1026, 118.0, 75.0, 68, 4.9, '钙片，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (7, 1027, 145.0, 88.0, 82, 6.5, '阿司匹林，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (8, 1028, 138.0, 82.0, 75, 5.4, '速效救心丸，必要时服', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (9, 1021, 132.0, 83.0, 70, 5.3, '阿司匹林，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (10, 1022, 138.0, 88.0, 76, 6.0, '硝苯地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (11, 1024, 142.0, 90.0, 80, 7.1, '二甲双胍，每日三次，增加胰岛素', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (12, 1029, 138.0, 85.0, 72, 5.8, '硝苯地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (13, 1029, 132.0, 82.0, 70, 5.6, '硝苯地平，每日一次，血压控制良好', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (14, 1030, 150.0, 95.0, 80, 7.5, '二甲双胍，每日二次；厄贝沙坦', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (15, 1030, 142.0, 88.0, 75, 6.9, '二甲双胍，每日二次；厄贝沙坦', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (16, 1031, 125.0, 75.0, 68, 4.8, '目前无需特殊用药，建议保持运动', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (17, 1031, 128.0, 78.0, 70, 5.0, '目前无需特殊用药，注意保暖', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (18, 1032, 145.0, 88.0, 82, 6.1, '氨氯地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (19, 1032, 136.0, 82.0, 78, 5.9, '氨氯地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (20, 1033, 130.0, 80.0, 72, 8.8, '格列齐特，每日一次，严格控糖', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (21, 1033, 128.0, 78.0, 70, 7.5, '格列齐特，每日一次，血糖有所下降', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (22, 1034, 118.0, 70.0, 65, 5.2, '骨化三醇及钙片，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (23, 1034, 120.0, 72.0, 68, 5.1, '骨化三醇及钙片，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (24, 1035, 145.0, 90.0, 92, 5.5, '倍他乐克，每日一次，控制心率', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (25, 1035, 138.0, 85.0, 82, 5.4, '倍他乐克，每日一次，心率平稳', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (26, 1036, 135.0, 85.0, 75, 6.8, '阿托伐他汀，每晚一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (27, 1036, 130.0, 82.0, 72, 6.2, '阿托伐他汀，每晚一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (28, 1037, 122.0, 78.0, 70, 4.9, '指标正常，建议适量散步', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (29, 1037, 125.0, 80.0, 72, 5.1, '指标正常，继续保持', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (30, 1038, 165.0, 105.0, 88, 7.2, '厄贝沙坦氢氯噻嗪，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (31, 1038, 148.0, 95.0, 82, 6.8, '厄贝沙坦氢氯噻嗪，血压开始回落', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (32, 1039, 130.0, 80.0, 75, 5.4, '阿司匹林、速效救心丸随身携带', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (33, 1039, 132.0, 82.0, 72, 5.3, '按时服药，近期无胸闷症状', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (34, 1040, 140.0, 88.0, 80, 10.5, '胰岛素注射，每日二次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (35, 1040, 138.0, 85.0, 78, 8.2, '胰岛素注射，空腹血糖改善', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (36, 1041, 138.0, 82.0, 68, 5.0, '非洛地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (37, 1041, 130.0, 78.0, 66, 4.9, '非洛地平，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (38, 1042, 145.0, 92.0, 95, 6.5, '美托洛尔，每日二次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (39, 1042, 140.0, 88.0, 82, 6.2, '美托洛尔，心率恢复正常范围', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (40, 1043, 135.0, 82.0, 76, 5.8, '辅酶Q10，阿司匹林', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (41, 1043, 132.0, 80.0, 74, 5.5, '辅酶Q10，阿司匹林，维持原方案', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (42, 1044, 155.0, 95.0, 88, 9.2, '阿卡波糖 餐时服用；硝苯地平', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (43, 1044, 145.0, 88.0, 84, 7.8, '按原方案服药，各项指标改善', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (44, 1045, 125.0, 78.0, 70, 5.2, '碳酸钙D3，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (45, 1045, 122.0, 75.0, 68, 5.0, '碳酸钙D3，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (46, 1046, 138.0, 85.0, 75, 6.0, '辛伐他汀，每晚一次，清淡饮食', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (47, 1046, 135.0, 82.0, 72, 5.8, '辛伐他汀，坚持低脂饮食有效', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (48, 1047, 142.0, 88.0, 82, 7.5, '二甲双胍，每日一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (49, 1047, 138.0, 85.0, 80, 6.6, '二甲双胍，血糖控制在合格线', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (50, 1048, 130.0, 80.0, 65, 5.5, '稳心颗粒，每日三次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (51, 1048, 128.0, 78.0, 68, 5.4, '稳心颗粒，症状缓解', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (52, 1021, 134.0, 82.0, 71, 5.3, '阿司匹林，每日一次，复查情况稳定', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (53, 1021, 130.0, 80.0, 70, 5.2, '阿司匹林，每日一次，血压极佳', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (54, 1022, 136.0, 85.0, 75, 5.9, '硝苯地平，每日一次，减量测试', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (55, 1022, 135.0, 84.0, 74, 5.8, '硝苯地平，维持减量后状态良好', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (56, 1023, 124.0, 78.0, 64, 5.0, '目前无需用药，日常随访', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (57, 1023, 122.0, 75.0, 62, 4.9, '血压心率完美，建议继续跳广场舞', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (58, 1023, 120.0, 72.0, 60, 4.8, '常规体检，未见异常', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (59, 1024, 140.0, 88.0, 78, 6.8, '二甲双胍，增加胰岛素后血糖下降', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (60, 1024, 138.0, 85.0, 75, 6.5, '二甲双胍，胰岛素减量', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (61, 1024, 135.0, 82.0, 72, 6.2, '二甲双胍，每日三次，状态恢复不错', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (62, 1025, 128.0, 82.0, 68, 5.6, '阿托伐他汀，每晚一次', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (63, 1025, 125.0, 80.0, 66, 5.4, '阿托伐他汀，血脂复查已达标', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (64, 1025, 126.0, 81.0, 67, 5.5, '阿托伐他汀，隔日一次维持', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (65, 1026, 115.0, 72.0, 65, 4.8, '钙片，每日一次，骨密度复查', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (66, 1026, 118.0, 74.0, 66, 4.9, '钙片，每日一次，关节疼痛缓解', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (67, 1026, 120.0, 75.0, 68, 5.0, '停用钙片，改为食补', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (68, 1027, 142.0, 85.0, 80, 6.3, '阿司匹林，每日一次，微调剂量', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (69, 1027, 140.0, 82.0, 78, 6.1, '阿司匹林，每日一次，情况平稳', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (70, 1028, 135.0, 80.0, 72, 5.2, '速效救心丸，必要时服，近期未发作', '2026-04-03 21:34:03');
INSERT INTO `health_records` VALUES (71, 1028, 132.0, 78.0, 70, 5.0, '常规心电图复查正常，继续观察', '2026-04-03 21:34:03');

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

SET FOREIGN_KEY_CHECKS = 1;
