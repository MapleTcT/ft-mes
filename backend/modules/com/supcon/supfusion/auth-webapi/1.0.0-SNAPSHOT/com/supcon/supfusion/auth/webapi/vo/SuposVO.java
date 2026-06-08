package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SuposVO extends VO {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer code;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String message;

}
