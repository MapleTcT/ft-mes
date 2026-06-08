package com.supcon.supfusion.organization.service.bo.group;

import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupBO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String fullPath;
    private double sort;
    private Long companyId;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;
}
