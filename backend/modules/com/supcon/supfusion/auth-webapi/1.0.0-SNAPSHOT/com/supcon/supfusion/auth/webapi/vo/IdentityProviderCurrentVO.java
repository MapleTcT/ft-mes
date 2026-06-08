package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class IdentityProviderCurrentVO extends VO {

    private List<External> external;

    private List<Internal> internal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Valid
    public static class External {

        private String oauthName;

        private String auth_url;

        private String logout_url;

        private String qrcode_url;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Valid
    public static class Internal {

        private String oauthName;

        private String auth_url;
    }
}

