package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonLeaderDTO extends DTO {

    private PersonDTO directLeader;

    private PersonDTO grandLeader;
}
