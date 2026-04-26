package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("doctor_work_pattern")
public class DoctorWorkPattern {
    @TableId(type = IdType.AUTO)
    private Long patternId;
    private Long doctorId;
    /** 1-7 => 周一到周日 */
    private Integer weekday;
    /** 0上午, 1下午 */
    private Integer timeSlot;
    private Integer defaultCapacity;
    /** 1启用, 0停用 */
    private Integer active;
}
