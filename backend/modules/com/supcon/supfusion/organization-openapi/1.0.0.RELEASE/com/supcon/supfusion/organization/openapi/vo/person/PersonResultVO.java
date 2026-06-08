package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentResultBO;
import com.supcon.supfusion.organization.service.bo.person.UserBO;
import com.supcon.supfusion.organization.service.bo.position.PositionResultBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonResultVO extends VO {

    /**
     * 人员id
     */
    private Long id;

    /**
     * 人员编码
     */
    private String code;

    /**
     * 人员名称
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 主岗id
     */
    private Long mainPositionId;


    /**
     * 用户账号信息
     */
    private UserBO account;

    /**
     * 所属部门
     */
    private List<DepartmentResultBO> departments;

    /**
     * 所属岗位
     */
    private List<PositionResultBO> positions;
}
