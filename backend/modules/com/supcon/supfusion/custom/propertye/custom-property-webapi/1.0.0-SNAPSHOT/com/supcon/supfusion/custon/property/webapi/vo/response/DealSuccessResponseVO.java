package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

import java.util.Map;

/**
 * @author zhang yafei
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealSuccessResponseVO extends VO {

    private Boolean dealSuccessFlag;

    private String propDisplayName;

    private String displayValue;

    private Map<String, Object> relationMap;
}
