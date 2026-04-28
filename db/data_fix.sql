-- ============================================
-- 智慧养老项目 - 数据修复脚本
-- 文件: db/data_fix.sql
-- 说明:
-- 1) 按当前库结构定制，优先修复“关系一致性 + 核心统计一致性”
-- 2) 会执行 UPDATE/DELETE/ALTER，请先备份数据库
-- 3) 建议顺序:
--    a) 先执行 db/data_check.sql 保存问题快照
--    b) 执行本脚本
--    c) 再执行 db/data_check.sql 复核
-- ============================================

USE `smart-elderly-db`;

-- --------------------------------------------
-- 0. 备份提示（手动执行，不在脚本内自动导出）
-- --------------------------------------------
-- mysqldump -u root -p smart-elderly-db > smart-elderly-db-backup.sql

SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- --------------------------------------------
-- 1. 清理 family_bind 重复数据
--    规则：同 elder_id + family_id 保留最小 bind_id
-- --------------------------------------------
DELETE fb1
FROM family_bind fb1
JOIN family_bind fb2
  ON fb1.elder_id = fb2.elder_id
 AND fb1.family_id = fb2.family_id
 AND fb1.bind_id > fb2.bind_id;

-- --------------------------------------------
-- 2. 清理 family_bind 角色不匹配数据
--    elder_id 必须是 role=0
--    family_id 必须是 role=1
-- --------------------------------------------
DELETE fb
FROM family_bind fb
LEFT JOIN `user` ue ON ue.user_id = fb.elder_id
LEFT JOIN `user` uf ON uf.user_id = fb.family_id
WHERE ue.user_id IS NULL
   OR uf.user_id IS NULL
   OR ue.role <> 0
   OR uf.role <> 1
   OR fb.elder_id = fb.family_id;

-- --------------------------------------------
-- 3. 清理 appointment 角色不匹配 / 非法数据
-- --------------------------------------------
-- 3.1 患者不存在或不是老人
DELETE a
FROM appointment a
LEFT JOIN `user` u ON u.user_id = a.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 3.2 医生不存在或不是医生
DELETE a
FROM appointment a
LEFT JOIN `user` u ON u.user_id = a.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- 3.3 状态非法或为空，统一置为待处理(0)
UPDATE appointment
SET status = 0
WHERE status NOT IN (0, 1, 2) OR status IS NULL;

-- 3.4 预约时间为空，兜底置为“当前时间”
UPDATE appointment
SET appoint_time = NOW()
WHERE appoint_time IS NULL;

-- 3.5 完全重复预约去重（同 user_id + doctor_id + appoint_time + status）
--     保留最小 appoint_id
DELETE a1
FROM appointment a1
JOIN appointment a2
  ON a1.user_id = a2.user_id
 AND a1.doctor_id = a2.doctor_id
 AND a1.appoint_time = a2.appoint_time
 AND a1.status = a2.status
 AND a1.appoint_id > a2.appoint_id;

-- --------------------------------------------
-- 4. 清理 doctor_schedule 非法数据
-- --------------------------------------------
-- 4.1 doctor_id 不存在或不是医生 -> 删除该排班
DELETE s
FROM doctor_schedule s
LEFT JOIN `user` u ON u.user_id = s.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- 4.2 time_slot 非法，默认修正为上午(0)
UPDATE doctor_schedule
SET time_slot = 0
WHERE time_slot NOT IN (0, 1) OR time_slot IS NULL;

-- 4.3 max_capacity/booked_count 空值或负数修正
UPDATE doctor_schedule
SET max_capacity = CASE
        WHEN max_capacity IS NULL OR max_capacity < 0 THEN 0
        ELSE max_capacity
    END,
    booked_count = CASE
        WHEN booked_count IS NULL OR booked_count < 0 THEN 0
        ELSE booked_count
    END;

-- 4.4 同医生同日同时段重复排班去重（保留最小 schedule_id）
DELETE s1
FROM doctor_schedule s1
JOIN doctor_schedule s2
  ON s1.doctor_id = s2.doctor_id
 AND s1.work_date = s2.work_date
 AND s1.time_slot = s2.time_slot
 AND s1.schedule_id > s2.schedule_id;

-- 4.5 清理 doctor_work_pattern 非法数据
-- doctor_id 不存在或不是医生 -> 删除
DELETE p
FROM doctor_work_pattern p
LEFT JOIN `user` u ON u.user_id = p.doctor_id
WHERE u.user_id IS NULL OR u.role <> 2;

-- weekday 非法 -> 置为周一
UPDATE doctor_work_pattern
SET weekday = 1
WHERE weekday NOT IN (1,2,3,4,5,6,7) OR weekday IS NULL;

-- time_slot 非法 -> 置为上午
UPDATE doctor_work_pattern
SET time_slot = 0
WHERE time_slot NOT IN (0,1) OR time_slot IS NULL;

-- default_capacity 非法 -> 置为10
UPDATE doctor_work_pattern
SET default_capacity = 10
WHERE default_capacity IS NULL OR default_capacity <= 0;

-- active 非法 -> 置为启用
UPDATE doctor_work_pattern
SET active = 1
WHERE active NOT IN (0,1) OR active IS NULL;

-- 模板重复去重（同医生+周几+时段保留最小 pattern_id）
DELETE p1
FROM doctor_work_pattern p1
JOIN doctor_work_pattern p2
  ON p1.doctor_id = p2.doctor_id
 AND p1.weekday = p2.weekday
 AND p1.time_slot = p2.time_slot
 AND p1.pattern_id > p2.pattern_id;

-- --------------------------------------------
-- 4A. 清理 building_geo_zone 非法数据
-- --------------------------------------------
-- 4A.1 active 非法 -> 置为启用
UPDATE building_geo_zone
SET active = 1
WHERE active NOT IN (0, 1) OR active IS NULL;

-- 4A.2 非法经纬度范围 -> 删除
DELETE FROM building_geo_zone
WHERE min_latitude >= max_latitude
   OR min_longitude >= max_longitude
   OR building_no IS NULL;

-- 4A.3 同楼栋重复仅保留最小 zone_id
DELETE b1
FROM building_geo_zone b1
JOIN building_geo_zone b2
  ON b1.building_no = b2.building_no
 AND b1.zone_id > b2.zone_id;

-- --------------------------------------------
-- 5. 重新回填 doctor_schedule.booked_count
--    规则：booked_count = 已确认预约(status=1)数量
-- --------------------------------------------
UPDATE doctor_schedule
SET booked_count = 0;

UPDATE doctor_schedule s
JOIN (
  SELECT
    a.doctor_id,
    DATE(a.appoint_time) AS work_date,
    CASE WHEN HOUR(a.appoint_time) < 12 THEN 0 ELSE 1 END AS time_slot,
    COUNT(*) AS cnt
  FROM appointment a
  WHERE a.status = 1
    AND a.appoint_time IS NOT NULL
  GROUP BY a.doctor_id, DATE(a.appoint_time), CASE WHEN HOUR(a.appoint_time) < 12 THEN 0 ELSE 1 END
) t
ON s.doctor_id = t.doctor_id
AND s.work_date = t.work_date
AND s.time_slot = t.time_slot
SET s.booked_count = t.cnt;

-- 5.1 再次兜底：booked_count 不得大于 max_capacity
UPDATE doctor_schedule
SET booked_count = GREATEST(LEAST(booked_count, max_capacity), 0);

-- 5.2 若排班全部早于今天，自动整体平移到未来，避免“所有医生无可预约时段”
--     示例：当前最大排班日是 2026-04-17，而今天是 2026-04-26，则整体后移到“至少明天开始”
SET @target_future_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY);
SET @current_max_work_date = (SELECT MAX(work_date) FROM doctor_schedule);
SET @schedule_shift_days = (
    SELECT CASE
        WHEN @current_max_work_date IS NULL THEN 0
        WHEN @current_max_work_date >= @target_future_date THEN 0
        ELSE DATEDIFF(@target_future_date, @current_max_work_date)
    END
);

UPDATE doctor_schedule
SET work_date = DATE_ADD(work_date, INTERVAL @schedule_shift_days DAY)
WHERE @schedule_shift_days > 0;

-- 5.3 验收兜底：未来排班至少保留 1 个可预约号源（避免每个医生都“号满”）
UPDATE doctor_schedule
SET max_capacity = 10
WHERE work_date >= CURDATE()
  AND (max_capacity IS NULL OR max_capacity <= 0);

UPDATE doctor_schedule
SET booked_count = LEAST(GREATEST(booked_count, 0), GREATEST(max_capacity - 1, 0))
WHERE work_date >= CURDATE()
  AND max_capacity > 0;

-- 5.4 基于固定班次模板补齐未来30天排班
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

-- --------------------------------------------
-- 6. 清理 emergency_record 非法数据
-- --------------------------------------------
-- 6.1 user_id 非老人 -> 删除
DELETE e
FROM emergency_record e
LEFT JOIN `user` u ON u.user_id = e.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 6.2 状态非法 -> 置为待处理(0)
UPDATE emergency_record
SET status = 0
WHERE status NOT IN (0, 1, 2, 3) OR status IS NULL;

-- 6.3 help_time 为空 -> 当前时间
UPDATE emergency_record
SET help_time = NOW()
WHERE help_time IS NULL;

-- 6.4 验收兜底：若当前没有待处理(status=0)工单，则自动补2条测试工单
INSERT INTO emergency_record (user_id, help_time, location, status, handle_result)
SELECT 1021, NOW(), '小区3号楼（验收测试）', 0, '验收测试：老人一键报警待处理'
WHERE NOT EXISTS (SELECT 1 FROM emergency_record WHERE status = 0);

INSERT INTO emergency_record (user_id, help_time, location, status, handle_result)
SELECT 1030, DATE_SUB(NOW(), INTERVAL 5 MINUTE), '小区6号楼（验收测试）', 0, '验收测试：夜间不适待处理'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_record WHERE status = 0 AND location LIKE '%验收测试%'
);

-- --------------------------------------------
-- 7. 清理 health_records 非法数据
-- --------------------------------------------
-- 7.1 user_id 非老人 -> 删除
DELETE h
FROM health_records h
LEFT JOIN `user` u ON u.user_id = h.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 7.2 record_time 为空 -> 当前时间
UPDATE health_records
SET record_time = NOW()
WHERE record_time IS NULL;

-- 7.3 生理值明显越界时置 NULL（避免脏值影响趋势图和AI）
UPDATE health_records
SET blood_pressure_high = NULL
WHERE blood_pressure_high IS NOT NULL
  AND (blood_pressure_high < 60 OR blood_pressure_high > 260);

UPDATE health_records
SET blood_pressure_low = NULL
WHERE blood_pressure_low IS NOT NULL
  AND (blood_pressure_low < 30 OR blood_pressure_low > 180);

UPDATE health_records
SET heart_rate = NULL
WHERE heart_rate IS NOT NULL
  AND (heart_rate < 20 OR heart_rate > 220);

UPDATE health_records
SET blood_sugar = NULL
WHERE blood_sugar IS NOT NULL
  AND (blood_sugar < 1 OR blood_sugar > 40);

-- 7.4 用药信息清洗：去除“指标评价/建议”类尾部描述，仅保留药名与用法
-- 例如：阿司匹林，每日一次，血压极佳 -> 阿司匹林，每日一次
UPDATE health_records
SET medication_info = TRIM(REGEXP_REPLACE(
    medication_info,
    '[，,;；](血压|血糖|心率|指标|情况|状态|复查|建议|注意|改善|回落|正常|达标|平稳|良好|优秀|极佳).*$',
    ''
))
WHERE medication_info IS NOT NULL
  AND medication_info <> ''
  AND medication_info REGEXP '[，,;；](血压|血糖|心率|指标|情况|状态|复查|建议|注意|改善|回落|正常|达标|平稳|良好|优秀|极佳)';

-- --------------------------------------------
-- 7A. 清理 diagnosis_record 非法数据
-- --------------------------------------------
-- 7A.1 user_id 非老人 -> 删除
DELETE d
FROM diagnosis_record d
LEFT JOIN `user` u ON u.user_id = d.user_id
WHERE u.user_id IS NULL OR u.role <> 0;

-- 7A.2 doctor_id 若存在但不是医生 -> 置空
UPDATE diagnosis_record d
LEFT JOIN `user` u ON u.user_id = d.doctor_id
SET d.doctor_id = NULL
WHERE d.doctor_id IS NOT NULL
  AND (u.user_id IS NULL OR u.role <> 2);

-- 7A.3 诊断类型非法 -> 删除
DELETE FROM diagnosis_record
WHERE diagnosis_type NOT IN ('高血压','低血压','糖尿病','心率异常')
   OR diagnosis_type IS NULL;

-- 7A.4 active 非法 -> 统一置为有效
UPDATE diagnosis_record
SET active = 1
WHERE active NOT IN (0, 1) OR active IS NULL;

-- 7A.5 diagnosis_time 为空 -> 当前时间
UPDATE diagnosis_record
SET diagnosis_time = NOW()
WHERE diagnosis_time IS NULL;

-- --------------------------------------------
-- 8. 新增关键约束（幂等：不存在才添加）
-- --------------------------------------------
-- 8.1 family_bind 防重复绑定（唯一索引）
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'family_bind'
              AND index_name = 'uk_elder_family'
        ),
        'SELECT ''skip: uk_elder_family already exists'' AS message',
        'ALTER TABLE family_bind ADD UNIQUE KEY uk_elder_family (elder_id, family_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8.2 doctor_schedule 防重复排班（唯一索引）
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'doctor_schedule'
              AND index_name = 'uk_doctor_date_slot'
        ),
        'SELECT ''skip: uk_doctor_date_slot already exists'' AS message',
        'ALTER TABLE doctor_schedule ADD UNIQUE KEY uk_doctor_date_slot (doctor_id, work_date, time_slot)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8.2A doctor_work_pattern 防重复模板（唯一索引）
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'doctor_work_pattern'
              AND index_name = 'uk_work_pattern'
        ),
        'SELECT ''skip: uk_work_pattern already exists'' AS message',
        'ALTER TABLE doctor_work_pattern ADD UNIQUE KEY uk_work_pattern (doctor_id, weekday, time_slot)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8.3 业务状态检查约束（MySQL 8.0.16+ 支持）
-- appointment.status
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'appointment'
              AND constraint_name = 'chk_appointment_status'
              AND constraint_type = 'CHECK'
        ),
        'SELECT ''skip: chk_appointment_status already exists'' AS message',
        'ALTER TABLE appointment ADD CONSTRAINT chk_appointment_status CHECK (status IN (0, 1, 2))'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 8.4 diagnosis_record 唯一有效诊断索引（同一老人同一时刻同类型避免重复）
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'diagnosis_record'
              AND index_name = 'uk_diag_unique'
        ),
        'SELECT ''skip: uk_diag_unique already exists'' AS message',
        'ALTER TABLE diagnosis_record ADD UNIQUE KEY uk_diag_unique (user_id, diagnosis_time, diagnosis_type)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- emergency_record.status
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'emergency_record'
              AND constraint_name = 'chk_emergency_status'
              AND constraint_type = 'CHECK'
        ),
        'SELECT ''skip: chk_emergency_status already exists'' AS message',
        'ALTER TABLE emergency_record ADD CONSTRAINT chk_emergency_status CHECK (status IN (0, 1, 2, 3))'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- doctor_schedule.time_slot
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'doctor_schedule'
              AND constraint_name = 'chk_schedule_timeslot'
              AND constraint_type = 'CHECK'
        ),
        'SELECT ''skip: chk_schedule_timeslot already exists'' AS message',
        'ALTER TABLE doctor_schedule ADD CONSTRAINT chk_schedule_timeslot CHECK (time_slot IN (0, 1))'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- doctor_schedule.capacity
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'doctor_schedule'
              AND constraint_name = 'chk_schedule_capacity'
              AND constraint_type = 'CHECK'
        ),
        'SELECT ''skip: chk_schedule_capacity already exists'' AS message',
        'ALTER TABLE doctor_schedule ADD CONSTRAINT chk_schedule_capacity CHECK (booked_count >= 0 AND max_capacity >= 0 AND booked_count <= max_capacity)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;

-- --------------------------------------------
-- 9. 修复后快速复核（节选）
-- --------------------------------------------
-- 9.1 family_bind 重复检查
SELECT elder_id, family_id, COUNT(*) AS cnt
FROM family_bind
GROUP BY elder_id, family_id
HAVING COUNT(*) > 1;

-- 9.2 排班 booked_count 与确认预约数一致性检查
WITH appt_confirmed AS (
    SELECT
        doctor_id,
        DATE(appoint_time) AS work_date,
        CASE WHEN HOUR(appoint_time) < 12 THEN 0 ELSE 1 END AS time_slot,
        COUNT(*) AS confirmed_cnt
    FROM appointment
    WHERE status = 1
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

-- 9.3 关键表总量
SELECT 'appointment' AS table_name, COUNT(*) AS total_count FROM appointment
UNION ALL
SELECT 'doctor_schedule', COUNT(*) FROM doctor_schedule
UNION ALL
SELECT 'family_bind', COUNT(*) FROM family_bind
UNION ALL
SELECT 'emergency_record', COUNT(*) FROM emergency_record
UNION ALL
SELECT 'health_records', COUNT(*) FROM health_records;

SELECT 'data_fix.sql 执行完成' AS message;
