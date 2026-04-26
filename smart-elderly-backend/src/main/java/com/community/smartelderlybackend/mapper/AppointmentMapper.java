package com.community.smartelderlybackend.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.smartelderlybackend.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select("""
            SELECT
                DATE(appoint_time) AS workDate,
                CASE WHEN HOUR(appoint_time) < 12 THEN 0 ELSE 1 END AS timeSlot,
                status AS status,
                COUNT(*) AS total
            FROM appointment
            WHERE doctor_id = #{doctorId}
              AND status IN (0, 1)
              AND appoint_time BETWEEN #{startTime} AND #{endTime}
            GROUP BY DATE(appoint_time), CASE WHEN HOUR(appoint_time) < 12 THEN 0 ELSE 1 END, status
            """)
    List<Map<String, Object>> selectScheduleStatsGrouped(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}