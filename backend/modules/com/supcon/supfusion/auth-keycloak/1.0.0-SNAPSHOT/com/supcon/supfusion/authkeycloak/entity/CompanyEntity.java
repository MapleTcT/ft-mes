package com.supcon.supfusion.authkeycloak.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
public class CompanyEntity {

    /**
     * 公司id
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long id;

    /**
     * 公司编码
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String code;

    /**
     * 集团或公司简称
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String shortName;

    /**
     * 集团或公司全称
     */
    private String fullName;

    /**
     * 公司全路径
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String fullPath;

    /**
     * 节点层级
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double sort;

    /**
     * 父级节点id
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long parentId;

    /**
     * 描述
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String password;
}
