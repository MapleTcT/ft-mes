package com.supcon.supfusion.systemcode.service;

import com.supcon.supfusion.module.registry.dto.ModuleDTO;

import java.util.Collection;
import java.util.List;

public interface ModuleService {

    Collection<ModuleDTO> queryModuleList();

    ModuleDTO queryModuleByModuleId(String moduleId);

    List<String> queryModuleByAppId(String appId);
}
