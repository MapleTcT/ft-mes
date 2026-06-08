package com.supcon.supfusion.i18n.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nResourceDownloadService {

    PageResult judgeGetModuleResource(String moduleCode);

    PageResult getModuleResource(Map map);

    String getModulesResource(List list);

    PageResult<Map<String, Map<String, String>>> getModulesResourceKeyValues(List<String> list,String languCode);

    Result<Map<String, Map<String, String>>> judgeGetModuleResource2(Collection<String> moduleIds);

    Result getModulesResourcesOpenApi(String[] moduleCodes);

    Result getModulesResourceIndexs(List<String> moduleCodes);
}
