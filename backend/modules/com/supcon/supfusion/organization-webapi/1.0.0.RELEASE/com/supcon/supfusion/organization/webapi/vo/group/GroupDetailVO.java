package com.supcon.supfusion.organization.webapi.vo.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

/**
 * 组详细信息
 *
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailVO extends VO {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long id;

    private String code;

    /**
     * 组名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;
}
