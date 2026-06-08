package com.supcon.supfusion.authkeycloak.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenResponse {
    @JsonProperty("code")
    protected Integer code;
    @JsonProperty("message")
    protected String message;
}
