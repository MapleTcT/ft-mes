package com.supcon.supfusion.organization.dao.po.company;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailInfoPO {

    private Long id;

    private String code;

    private String parentCode;

    private String fullName;

    private String shortName;

    private String description;

    private List<String> tags;

    private String fullPath;

    private String layNo;

    private String sort;

    private Integer valid;

    private String modifyTime;
}
