package com.supcon.supfusion.authkeycloak.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class LoginInfo {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String password;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String clientId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String language;
}
