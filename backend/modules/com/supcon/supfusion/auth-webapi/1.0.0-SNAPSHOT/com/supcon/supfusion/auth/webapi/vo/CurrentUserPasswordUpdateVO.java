package com.supcon.supfusion.auth.webapi.vo;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserPasswordUpdateVO extends VO {
    @NotEmpty
    private String password;
    @NotEmpty
    private String prepassword;
    @NotEmpty
    private String repassword;
}
