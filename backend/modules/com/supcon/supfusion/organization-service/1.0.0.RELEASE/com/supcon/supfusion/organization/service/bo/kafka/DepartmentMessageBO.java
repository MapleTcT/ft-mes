package com.supcon.supfusion.organization.service.bo.kafka;

import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMessageBO {

    private Long rowVersion;

    private Long id;

    private Long parentId;

    private String parentCode;

    private String code;

    private String name;

    private SystemCodeBO deptType;

    private String description;

    private String fullPath;

    private Integer layNo;

    private Double sort;

    private String modifyTime;

    private CompanySimpleMessageBO company;

    private List<ManagerMessageBO> managers;
}
