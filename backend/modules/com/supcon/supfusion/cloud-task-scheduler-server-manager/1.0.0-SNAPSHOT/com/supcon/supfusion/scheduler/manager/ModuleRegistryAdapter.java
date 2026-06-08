package com.supcon.supfusion.scheduler.manager;

import com.supcon.supfusion.module.registry.dto.ModuleDTO;

import java.util.Collection;

/**
 * @author caokele
 */
public interface ModuleRegistryAdapter {

    Collection<ModuleDTO> queryModules();
}
