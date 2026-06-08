package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonLeaderBO {

    private PersonBO directLeader;

    private PersonBO grandLeader;
}
