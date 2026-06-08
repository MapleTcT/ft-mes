package com.supcon.supfusion.configuration.services.projectapi.services;

import java.io.File;
import java.util.List;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
public interface ProjImportExportAdmin {
    void importProj(File configFile, List<String> filter);

    void exportProj(List<String> moduleCodes);

    String getZipFilePath();
}
