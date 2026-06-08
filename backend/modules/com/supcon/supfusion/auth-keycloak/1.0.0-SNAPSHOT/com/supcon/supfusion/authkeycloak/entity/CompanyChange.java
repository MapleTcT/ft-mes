package com.supcon.supfusion.authkeycloak.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class CompanyChange {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String clientId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
}
