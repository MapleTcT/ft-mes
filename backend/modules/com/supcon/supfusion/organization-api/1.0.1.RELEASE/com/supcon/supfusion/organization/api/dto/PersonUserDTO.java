package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonUserDTO extends DTO {

    private Long personId;

    private Long userId;

    private String userName;
}
