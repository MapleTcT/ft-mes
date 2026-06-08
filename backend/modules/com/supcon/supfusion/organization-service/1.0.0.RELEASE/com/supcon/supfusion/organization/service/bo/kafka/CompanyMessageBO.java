package com.supcon.supfusion.organization.service.bo.kafka;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMessageBO {

    private Long rowVersion;

    private Long id;

    private String code;

    private Long parentId;

    private String parentCode;

    private String fullName;

    private String shortName;

    private String description;

    private List<String> tags;

    private String fullPath;

    private Integer layNo;

    private Double sort;
}
