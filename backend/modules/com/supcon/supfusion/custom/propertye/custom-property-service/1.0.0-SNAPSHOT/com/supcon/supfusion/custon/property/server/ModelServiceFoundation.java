package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.CustomPropertyModel;
import com.supcon.supfusion.custon.property.dao.entity.Model;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyModelBO;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ModelServiceFoundation {
    List<Model> getModels(String entityCode);

    List<CustomPropertyModelBO> findCustomPropertyModelMappings(String modelCode);

    CustomPropertyModel generateCustomPropertyModelMapping(Property p, Boolean enabled);

    void saveCustomPropertyModelMapping(CustomPropertyModelBO customPropertyModelMappingBO);

    Property getProperty(String code);

    Model getModel(String code);

    List<Model> getModelBycode(String code);

    void enableProperty(List<String> codes, List<Long> ids, Boolean enabled);

    Property getPKProperty(String modelCode);
}
