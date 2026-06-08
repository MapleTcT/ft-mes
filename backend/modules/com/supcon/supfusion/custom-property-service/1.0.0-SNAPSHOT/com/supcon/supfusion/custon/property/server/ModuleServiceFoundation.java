package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.Module;
import com.supcon.supfusion.custon.property.dao.entity.ModuleRelation;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ModuleServiceFoundation {
    List<Module> getAllModule();
    Module getModuleByCode(String code);

    List<Module> getModuleRelaton(String moduleCode);

    List<Module> getReferences(String moduleCode);
}
