package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

/**
 * 负责人
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationManagerBO {

    /**
     * 负责人id
     */
    private Long managerId;

    /**
     * 负责人name
     */
    private String managerName;

    /**
     * 负责人codde
     */
    private String managerCode;
}
