package com.supcon.supfusion.auth.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
public class UserFlowInfoDTO extends DTO {

    private Long id;

    private String userName;

    private Long personId;

}
