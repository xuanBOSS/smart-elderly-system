package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family_bind")
public class FamilyBind {
    @TableId(type = IdType.AUTO)
    private Long bindId;
    private Long elderId;
    private Long familyId;
    private String relation;
    private LocalDateTime createTime;
}