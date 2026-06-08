package com.supcon.supfusion.organization.service.bo.company;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司编码
     */
    private String code;

    /**
     * 集团或公司简称
     */
    private String shortName;

    /**
     * 集团或公司全称
     */
    private String fullName;

    /**
     * 公司全路径
     */
    private String fullPath;

    /**
     * 节点层级
     */
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    private Double sort;

    /**
     * 父级节点id
     */
    private Long parentId;

    /**
     * 描述
     */
    private String description;
}
