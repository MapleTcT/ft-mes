package com.supcon.supfusion.custon.property.server.bo;

import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.dao.entity.CustomPropertyView;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import lombok.Data;

import java.util.List;

/**
 * @author zhang yafei
 */
@Data
public class CustomPropertyViewBO extends CustomPropertyView {
    private Boolean isParent = false;
    private Property property;
    private String LayRec;
    private String _parentCode;
    private String _code;
    private ViewType viewType;
    private List<CustomPropertyViewBO> list;
}
