package com.supcon.supfusion.organization.service.bo.person;


import lombok.*;

/**
 * 人员修改页面的主岗下拉
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonPositionBO {

    /**
     * 岗位id
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 岗位编码
     */
    private String code;

    /**
     * 是否主岗
     */
    private Boolean mainPosition = false;

    /**
     * 岗位全路径
     */
    private String fullPath;

}
