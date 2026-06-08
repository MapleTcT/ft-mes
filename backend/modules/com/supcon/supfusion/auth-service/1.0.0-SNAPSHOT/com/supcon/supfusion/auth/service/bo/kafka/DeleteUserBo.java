package com.supcon.supfusion.auth.service.bo.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DeleteUserBo {
    @JsonProperty("rowVersion")
    private Integer rowVersion;
    @JsonProperty("name")
    private String name;
}
