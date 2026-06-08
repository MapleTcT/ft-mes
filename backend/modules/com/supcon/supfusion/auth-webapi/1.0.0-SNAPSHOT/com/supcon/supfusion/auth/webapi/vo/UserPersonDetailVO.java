package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPersonDetailVO extends VO {
    private Long id;
    private String userName;
    private Long personId;
    private String personName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RoleVO> role;
    private String timeZone;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String description;
    private String personCode;
    private String companyName;
    private Integer userType;
    private String phone;
    private String email;

}
