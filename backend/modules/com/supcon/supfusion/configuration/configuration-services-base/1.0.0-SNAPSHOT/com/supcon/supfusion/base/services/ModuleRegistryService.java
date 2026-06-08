package com.supcon.supfusion.base.services;

public interface ModuleRegistryService {

    /**
     * moduleCode xxx_1.0.0
     */
    void registryModule(String moduleCode);

    void deleteModule(String moduleCode);
}
