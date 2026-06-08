package com.supcon.supfusion.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOrgDetailDTO extends DTO {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean hasLock;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean valid;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String password;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Company> companies;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long personId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String phone;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String email;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer userType;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long userDirectoryId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ldapUserName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long companyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long positionId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String positionName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String positionCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long positionCompanyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long departmentId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String departmentName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String departmentCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyType;

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
