package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class PersonUserDTO extends DTO {

    private Long personId;

    private Long userId;

    private String userName;
}
