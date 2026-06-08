package com.supcon.supfusion.rbac.manager.impl;

import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.rbac.manager.IModuleAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
public class ModuleAdapterImpl implements IModuleAdapter {

    @Autowired
    private ModuleRegistryApi moduleRegistryApi;

    @Override
    public ModuleDTO getModule(String moduleId) {
        try {
            return moduleRegistryApi.getModule(moduleId);
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public Collection<ModuleDTO> queryModules() {
        try {
            return moduleRegistryApi.queryModules();
        }catch (Exception e){
            return null;
        }
    }


}
