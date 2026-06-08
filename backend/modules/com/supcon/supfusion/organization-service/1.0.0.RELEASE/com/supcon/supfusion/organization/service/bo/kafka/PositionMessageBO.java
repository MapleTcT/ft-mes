package com.supcon.supfusion.organization.service.bo.kafka;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionMessageBO {

    private Long rowVersion;

    private Long id;

    private Long parentId;

    private String parentCode;

    private String code;

    private String name;

    private String description;

    private String fullPath;

    private Integer layNo;

    private Double sort;

    private String modifyTime;

    private CompanySimpleMessageBO company;

    private DepartmentSimpleMessageBO department;
}
