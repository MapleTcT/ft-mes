package com.supcon.supfusion.configuration.services.projectapi.services;

import java.util.Map;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
public interface ProjBuildTplService {
    void buildTpl(String tpl, String basepath, String path, Map<String, Object> map);
}
