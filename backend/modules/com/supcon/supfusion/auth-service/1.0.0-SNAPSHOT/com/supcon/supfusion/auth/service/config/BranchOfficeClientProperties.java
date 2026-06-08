package com.supcon.supfusion.auth.service.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class BranchOfficeClientProperties {
    private String clientId;
    private String clientSecret;
}
