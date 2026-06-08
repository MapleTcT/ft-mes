package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.Field;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface FieldService {
    List<Field> getFields(String code);

    List<Field> getFieldsByDataGridCode(String code);
}
