package com.supcon.supfusion.custon.property.server.bo;

import com.supcon.supfusion.custon.property.dao.entity.CustomPropertyModel;
import com.supcon.supfusion.custon.property.dao.entity.Model;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import com.supcon.supfusion.custon.property.dao.entity.View;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
public class CustomPropertyModelBO extends CustomPropertyModel {
    private Property property;

    private Model model;

    private View refView; // 参照视图

    private Property associatedProperty; // 用于对象类型字段，记录关联字段
}
