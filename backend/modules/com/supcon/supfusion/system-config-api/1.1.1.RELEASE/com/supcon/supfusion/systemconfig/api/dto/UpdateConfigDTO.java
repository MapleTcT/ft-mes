package com.supcon.supfusion.systemconfig.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConfigDTO  {

    @NotEmpty(message = "moduleCode不能为空")
    String moduleCode;
    @Valid
    @NotEmpty(message = "配置项集合不能为空")
    List<ConfigInfoDTO> configInfoDTO;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigInfoDTO {
        @NotEmpty(message = "配置项key不能为空")
        String key;
        List<String> value;
    }
}
