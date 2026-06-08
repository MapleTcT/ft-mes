package com.supcon.supfusion.rbac.manager;

import com.supcon.supfusion.module.registry.dto.ModuleDTO;

import java.util.Collection;

public interface IModuleAdapter {

    ModuleDTO getModule( String moduleId);

    Collection<ModuleDTO> queryModules();

}
