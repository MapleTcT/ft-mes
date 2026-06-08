package com.supcon.supfusion.auth.service.bo.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@NoArgsConstructor
@Accessors(chain = true)
@Data
public class SaveAuthUserBo {

    @JsonProperty("rowVersion")
    private Integer rowVersion;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("person")
    private PersonDTO person;
    @JsonProperty("roleList")
    private List<RoleListDTO> roleList;

    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class PersonDTO {
        @JsonProperty("code")
        private String code;
        @JsonProperty("name")
        private String name;
    }

    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class RoleListDTO {
        @JsonProperty("code")
        private String code;
        @JsonProperty("name")
        private String name;
    }
}
