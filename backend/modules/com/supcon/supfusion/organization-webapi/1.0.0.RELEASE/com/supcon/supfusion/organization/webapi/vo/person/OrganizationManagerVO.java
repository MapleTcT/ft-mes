package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 负责人
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationManagerVO extends VO {

    /**
     * 负责人id
     */
    private Long managerId;

    /**
     * 负责人name
     */
    private String managerName;
}
