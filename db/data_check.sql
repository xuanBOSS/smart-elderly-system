-- ============================================
-- 智慧养老项目 - 数据体检脚本
-- 文件: db/data_check.sql
-- 说明:
-- 1) 仅查询，不修改数据
-- 2) 建议每次修复前后都执行一次并保存结果
-- ============================================

USE `smart-elderly-db`;

-- ----------------------------
-- 0. 基础总量检查
-- ----------------------------
SELECT 'user' AS table_name, COUNT(*) AS total_count FROM `user`
UNION ALL
SELECT 'appointment', COUNT(*) FROM appointment
UNION ALL
SELECT 'doctor_schedule', COUNT(*) FROM doctor_schedule
UNION ALL
SELECT 'doctor_work_pattern', COUNT(*) FROM doctor_work_pattern
UNION ALL
SELECT 'building_geo_zone', COUNT(*) FROM building_geo_zone
UNION ALL
SELECT 'family_bind', COUNT(*) FROM family_bind
UNION ALL
SELECT 'emergency_record', COUNT(*) FROM emergency_record
UNION ALL
SELECT 'health_records', COUNT(*) FROM health_records
UNION ALL
SELECT 'diagnosis_record', COUNT(*) FROM diagnosis_record;

-- 角色分布
SELECT role, COUNT(*) AS cnt
FROM `user`
GROUP BY role
ORDER BY role;

-- ----------------------------
-- 1. 用户基础数据问题
-- ----------------------------
-- 1.1 用户名重复（理论上有唯一索引，不应出现）
SELECT username, COUNT(*) AS cnt
FROM `user`
GROUP BY username
HAVING COUNT(*) > 1;

-- 1.2 角色非法（应为 0/1/2/3）
SELECT user_id, username, role
FROM `user`
WHERE role NOT IN (0, 1, 2, 3) OR role IS NULL;

-- 1.3 手机号长度异常（可按业务调整）
SELECT user_id, username, phone
FROM `user`
WHERE phone IS NOT NULL AND LENGTH(phone) <> 11;

-- ----------------------------
-- 2. appointment 关联与状态检查
-- ----------------------------
-- 2.1 患者 user_id 必须存在且角色=老人(0)
SELECT a.appoint_id, a.user_id, u.role AS user_role
FROM appointment a
LEFT JOIN `user` u ON u.user_id = a.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 2.2 医生 doctor_id 必须存在且角色=医生(2)
SELECT a.appoint_id, a.doctor_id, u.role AS doctor_role
FROM appointment a
LEFT JOIN `user` u ON u.user_id = a.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- 2.3 预约状态非法（应为 0/1/2）
SELECT appoint_id, user_id, doctor_id, status
FROM appointment
WHERE status NOT IN (0, 1, 2) OR status IS NULL;

-- 2.4 预约时间为空
SELECT appoint_id, user_id, doctor_id, appoint_time
FROM appointment
WHERE appoint_time IS NULL;

-- 2.5 同一老人同一医生同一时刻重复预约
SELECT user_id, doctor_id, appoint_time, COUNT(*) AS cnt
FROM appointment
GROUP BY user_id, doctor_id, appoint_time
HAVING COUNT(*) > 1;

-- ----------------------------
-- 3. doctor_schedule 一致性检查
-- ----------------------------
-- 3.1 doctor_id 必须存在且角色=医生(2)
SELECT s.schedule_id, s.doctor_id, u.role AS doctor_role
FROM doctor_schedule s
LEFT JOIN `user` u ON u.user_id = s.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- 3.2 time_slot 非法（应为 0/1）
SELECT schedule_id, doctor_id, work_date, time_slot
FROM doctor_schedule
WHERE time_slot NOT IN (0, 1) OR time_slot IS NULL;

-- 3.3 容量与已约人数非法
SELECT schedule_id, doctor_id, work_date, time_slot, max_capacity, booked_count
FROM doctor_schedule
WHERE max_capacity IS NULL
   OR booked_count IS NULL
   OR max_capacity < 0
   OR booked_count < 0
   OR booked_count > max_capacity;

-- 3.4 同一医生同一天同一时段重复排班
SELECT doctor_id, work_date, time_slot, COUNT(*) AS cnt
FROM doctor_schedule
GROUP BY doctor_id, work_date, time_slot
HAVING COUNT(*) > 1;

-- 3.5 排班 booked_count 与“已确认预约(status=1)”不一致
WITH appt_confirmed AS (
    SELECT
        doctor_id,
        DATE(appoint_time) AS work_date,
        CASE WHEN HOUR(appoint_time) < 12 THEN 0 ELSE 1 END AS time_slot,
        COUNT(*) AS confirmed_cnt
    FROM appointment
    WHERE status = 1 AND appoint_time IS NOT NULL
    GROUP BY doctor_id, DATE(appoint_time), CASE WHEN HOUR(appoint_time) < 12 THEN 0 ELSE 1 END
)
SELECT
    s.schedule_id,
    s.doctor_id,
    s.work_date,
    s.time_slot,
    s.booked_count,
    COALESCE(a.confirmed_cnt, 0) AS expected_booked_count
FROM doctor_schedule s
LEFT JOIN appt_confirmed a
       ON a.doctor_id = s.doctor_id
      AND a.work_date = s.work_date
      AND a.time_slot = s.time_slot
WHERE s.booked_count <> COALESCE(a.confirmed_cnt, 0);

-- 3.6 未来30天医生排班覆盖（用于验证自动生成）
SELECT
  doctor_id,
  COUNT(*) AS future_30_count
FROM doctor_schedule
WHERE work_date BETWEEN DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
GROUP BY doctor_id
ORDER BY doctor_id;

-- ----------------------------
-- 3B. doctor_work_pattern 检查
-- ----------------------------
-- 3B.1 doctor_id 必须存在且角色=医生(2)
SELECT p.pattern_id, p.doctor_id, u.role AS doctor_role
FROM doctor_work_pattern p
LEFT JOIN `user` u ON u.user_id = p.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- 3B.2 weekday 非法
SELECT pattern_id, doctor_id, weekday
FROM doctor_work_pattern
WHERE weekday NOT IN (1,2,3,4,5,6,7) OR weekday IS NULL;

-- 3B.3 time_slot 非法
SELECT pattern_id, doctor_id, time_slot
FROM doctor_work_pattern
WHERE time_slot NOT IN (0,1) OR time_slot IS NULL;

-- 3B.4 default_capacity 非法
SELECT pattern_id, doctor_id, default_capacity
FROM doctor_work_pattern
WHERE default_capacity IS NULL OR default_capacity <= 0;

-- 3B.5 模板重复
SELECT doctor_id, weekday, time_slot, COUNT(*) AS cnt
FROM doctor_work_pattern
GROUP BY doctor_id, weekday, time_slot
HAVING COUNT(*) > 1;

-- ----------------------------
-- 3A. building_geo_zone 检查
-- ----------------------------
-- 3A.1 楼栋号重复
SELECT building_no, COUNT(*) AS cnt
FROM building_geo_zone
GROUP BY building_no
HAVING COUNT(*) > 1;

-- 3A.2 经纬度范围非法
SELECT zone_id, building_no, min_latitude, max_latitude, min_longitude, max_longitude
FROM building_geo_zone
WHERE min_latitude >= max_latitude
   OR min_longitude >= max_longitude;

-- 3A.3 active 非法
SELECT zone_id, building_no, active
FROM building_geo_zone
WHERE active NOT IN (0,1) OR active IS NULL;

-- ----------------------------
-- 4. family_bind 关联与重复检查
-- ----------------------------
-- 4.1 elder_id 必须是老人(0)，family_id 必须是家属(1)
SELECT
    fb.bind_id,
    fb.elder_id,
    ue.role AS elder_role,
    fb.family_id,
    uf.role AS family_role
FROM family_bind fb
LEFT JOIN `user` ue ON ue.user_id = fb.elder_id
LEFT JOIN `user` uf ON uf.user_id = fb.family_id
WHERE ue.user_id IS NULL
   OR uf.user_id IS NULL
   OR ue.role <> 0
   OR uf.role <> 1;

-- 4.2 自绑定异常（老人ID与家属ID相同）
SELECT bind_id, elder_id, family_id
FROM family_bind
WHERE elder_id = family_id;

-- 4.3 重复绑定
SELECT elder_id, family_id, COUNT(*) AS cnt
FROM family_bind
GROUP BY elder_id, family_id
HAVING COUNT(*) > 1;

-- ----------------------------
-- 5. emergency_record 检查
-- ----------------------------
-- 5.1 user_id 必须存在且角色=老人(0)
SELECT e.help_id, e.user_id, u.role AS user_role
FROM emergency_record e
LEFT JOIN `user` u ON u.user_id = e.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 5.2 状态非法（应为 0/1/2/3）
SELECT help_id, user_id, status
FROM emergency_record
WHERE status NOT IN (0, 1, 2, 3) OR status IS NULL;

-- 5.2A 各状态工单数量（验收关注：status=0 待处理是否存在）
SELECT status, COUNT(*) AS cnt
FROM emergency_record
GROUP BY status
ORDER BY status;

-- 5.3 求助时间为空
SELECT help_id, user_id, help_time
FROM emergency_record
WHERE help_time IS NULL;

-- ----------------------------
-- 6. health_records 检查
-- ----------------------------
-- 6.1 user_id 必须存在且角色=老人(0)
SELECT h.record_id, h.user_id, u.role AS user_role
FROM health_records h
LEFT JOIN `user` u ON u.user_id = h.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 6.2 体征值明显异常（按常见生理范围，超出仅作“疑似脏数据”提示）
SELECT record_id, user_id, blood_pressure_high, blood_pressure_low, heart_rate, blood_sugar, record_time
FROM health_records
WHERE (blood_pressure_high IS NOT NULL AND (blood_pressure_high < 60 OR blood_pressure_high > 260))
   OR (blood_pressure_low  IS NOT NULL AND (blood_pressure_low  < 30 OR blood_pressure_low  > 180))
   OR (heart_rate          IS NOT NULL AND (heart_rate          < 20 OR heart_rate          > 220))
   OR (blood_sugar         IS NOT NULL AND (blood_sugar         < 1  OR blood_sugar         > 40));

-- 6.3 记录时间为空
SELECT record_id, user_id, record_time
FROM health_records
WHERE record_time IS NULL;

-- 6.4 同一老人同一时间完全重复记录
SELECT user_id, record_time, COUNT(*) AS cnt
FROM health_records
GROUP BY user_id, record_time
HAVING COUNT(*) > 1;

-- ----------------------------
-- 6A. diagnosis_record 检查
-- ----------------------------
-- 6A.1 user_id 必须存在且角色=老人(0)
SELECT d.diagnosis_id, d.user_id, u.role AS user_role
FROM diagnosis_record d
LEFT JOIN `user` u ON u.user_id = d.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 6A.2 doctor_id 若不为空，必须是医生(2)
SELECT d.diagnosis_id, d.doctor_id, u.role AS doctor_role
FROM diagnosis_record d
LEFT JOIN `user` u ON u.user_id = d.doctor_id
WHERE d.doctor_id IS NOT NULL AND (u.user_id IS NULL OR u.role <> 2);

-- 6A.3 诊断类型非法
SELECT diagnosis_id, user_id, diagnosis_type
FROM diagnosis_record
WHERE diagnosis_type NOT IN ('高血压','低血压','糖尿病','心率异常');

-- 6A.4 active 非法
SELECT diagnosis_id, user_id, active
FROM diagnosis_record
WHERE active NOT IN (0,1) OR active IS NULL;

-- 6A.5 诊断时间为空
SELECT diagnosis_id, user_id, diagnosis_time
FROM diagnosis_record
WHERE diagnosis_time IS NULL;

-- ----------------------------
-- 7. 跨表逻辑检查（业务规则）
-- ----------------------------
-- 7.1 已确认预约(status=1)找不到对应排班（医生、日期、时段）
WITH appt_slot AS (
    SELECT
        a.appoint_id,
        a.doctor_id,
        DATE(a.appoint_time) AS work_date,
        CASE WHEN HOUR(a.appoint_time) < 12 THEN 0 ELSE 1 END AS time_slot
    FROM appointment a
    WHERE a.status = 1 AND a.appoint_time IS NOT NULL
)
SELECT x.*
FROM appt_slot x
LEFT JOIN doctor_schedule s
       ON s.doctor_id = x.doctor_id
      AND s.work_date = x.work_date
      AND s.time_slot = x.time_slot
WHERE s.schedule_id IS NULL;

-- 7.2 预约时间早于 2020-01-01（可按项目时间线调整）
SELECT appoint_id, user_id, doctor_id, appoint_time
FROM appointment
WHERE appoint_time < '2020-01-01 00:00:00';

-- 7.3 健康记录时间早于 2020-01-01
SELECT record_id, user_id, record_time
FROM health_records
WHERE record_time < '2020-01-01 00:00:00';

-- 7.4 紧急求助时间早于 2020-01-01
SELECT help_id, user_id, help_time
FROM emergency_record
WHERE help_time < '2020-01-01 00:00:00';

-- 结束
SELECT 'data_check.sql 执行完成' AS message;
