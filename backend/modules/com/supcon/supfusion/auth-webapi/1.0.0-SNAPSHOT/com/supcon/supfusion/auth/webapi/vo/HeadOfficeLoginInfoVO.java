package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonPropertyOrder({"userName", "companyCode"})
public class HeadOfficeLoginInfoVO extends VO {
    private String userName;
    private String companyCode;
}
