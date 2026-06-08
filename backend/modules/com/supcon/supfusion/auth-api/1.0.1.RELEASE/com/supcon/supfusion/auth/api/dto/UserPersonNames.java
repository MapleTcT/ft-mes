package com.supcon.supfusion.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPersonNames extends DTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long personId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personName;
}
