package com.supcon.supfusion.custon.property.server.bo;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class ViewEnabledStatusCodeBO {

    private String propertyCode;
    private String associatedCode;
    private String propertyLayRec;
}
