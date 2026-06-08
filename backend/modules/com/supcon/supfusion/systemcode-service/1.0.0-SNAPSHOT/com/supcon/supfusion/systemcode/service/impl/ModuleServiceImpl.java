package com.supcon.supfusion.systemcode.service.impl;

import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.service.ModuleService;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class ModuleServiceImpl implements ModuleService {

    @Autowired
    ModuleRegistryApi moduleRegistryApi;

    @Override
    public Collection<ModuleDTO> queryModuleList() {
        Collection<ModuleDTO> moduleDTOList = moduleRegistryApi.queryModules();
        return filterSupideModule(moduleDTOList);
    }

    @Override
    public ModuleDTO queryModuleByModuleId(String moduleId) {
        ModuleDTO moduleDTO = moduleRegistryApi.getModule(moduleId);
        return moduleDTO;
    }

    @Override
    public List<String> queryModuleByAppId(String appId) {
        List<String> moduleIdList = moduleRegistryApi.singleApp(appId);
        return moduleIdList;
    }

    private Collection<ModuleDTO> filterSupideModule(Collection<ModuleDTO> moduleDTOList){
        if(CollectionUtils.isEmpty(moduleDTOList)){
           return moduleDTOList;
        }
        return moduleDTOList
            .stream()
            .filter(moduleDTO -> !moduleDTO.getModuleId().endsWith(Constants.SUPIDE))
            .collect(Collectors.toList());
    }


}
