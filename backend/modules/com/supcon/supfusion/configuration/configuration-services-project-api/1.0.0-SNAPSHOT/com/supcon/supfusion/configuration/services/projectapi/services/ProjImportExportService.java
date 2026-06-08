package com.supcon.supfusion.configuration.services.projectapi.services;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
public interface ProjImportExportService {
    void importProjConfig(String moduleCode,String path);
    void exportProjConfig(String moduleCode,String path);
}
