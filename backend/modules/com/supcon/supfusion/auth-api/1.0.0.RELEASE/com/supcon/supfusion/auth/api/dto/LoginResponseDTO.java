package com.supcon.supfusion.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import java.io.Serializable;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO extends DTO {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long userId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String username;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer userType;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LoginResponseDTO.Company> company;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long companyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Company implements Serializable {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Long companyId;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String companyName;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String companyCode;
    }

}
