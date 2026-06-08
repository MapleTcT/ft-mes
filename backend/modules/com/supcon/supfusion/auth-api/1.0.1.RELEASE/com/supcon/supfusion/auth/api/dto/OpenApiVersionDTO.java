package com.supcon.supfusion.auth.api.dto;


import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OpenApiVersionDTO extends DTO {
    private String version;
}
