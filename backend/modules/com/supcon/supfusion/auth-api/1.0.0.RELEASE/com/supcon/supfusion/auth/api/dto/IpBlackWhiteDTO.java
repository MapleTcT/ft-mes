package com.supcon.supfusion.auth.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * ip黑白名单
 *
 * @author caokele
 */
@Data
public class IpBlackWhiteDTO {
    /**
     * 企业Id
     */
    @NotNull(message = "企业Id不能为空")
    private Long companyId;
    /**
     * 访问IP
     */
    @NotEmpty(message = "访问IP不能为空")
    private String ip;
}
