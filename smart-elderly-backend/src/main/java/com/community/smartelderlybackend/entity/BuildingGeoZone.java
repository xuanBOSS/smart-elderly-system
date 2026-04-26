package com.community.smartelderlybackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("building_geo_zone")
public class BuildingGeoZone {
    @TableId(type = IdType.AUTO)
    private Long zoneId;
    private Integer buildingNo;     // 1~12
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
    private Integer active;         // 1启用 0停用
    private String note;
}

