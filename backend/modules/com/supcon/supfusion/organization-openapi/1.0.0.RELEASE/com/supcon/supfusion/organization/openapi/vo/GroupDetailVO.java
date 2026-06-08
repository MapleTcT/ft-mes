package com.supcon.supfusion.organization.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

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
     * 负责人id
     */
    private Long managerId;

    /**
     * 负责人名称
     */
    private String managerName;
}
