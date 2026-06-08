package com.supcon.supfusion.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
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
public class UserStaffDTO extends DTO {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String username;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long userId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long staffId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String staffName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String staffCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long companyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyName;
}
