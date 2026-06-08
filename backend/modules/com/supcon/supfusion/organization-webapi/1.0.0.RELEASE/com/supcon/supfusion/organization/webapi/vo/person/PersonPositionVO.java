package com.supcon.supfusion.organization.webapi.vo.person;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 人员关联的岗位
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonPositionVO extends VO {

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
