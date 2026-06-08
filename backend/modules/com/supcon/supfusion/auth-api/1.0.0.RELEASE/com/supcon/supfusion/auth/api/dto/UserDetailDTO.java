package com.supcon.supfusion.auth.api.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailDTO extends DTO {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long personId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long companyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean valid;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String password;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String phone;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String email;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyName;
    private Integer userType;

}
