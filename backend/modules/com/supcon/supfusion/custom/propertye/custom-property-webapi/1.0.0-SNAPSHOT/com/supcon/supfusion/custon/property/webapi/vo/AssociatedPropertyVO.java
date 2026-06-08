package com.supcon.supfusion.custon.property.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
public class AssociatedPropertyVO extends VO {

    private String code;

    private String moduleCode;

    private String entityCode;

    private String modelCode;
}
