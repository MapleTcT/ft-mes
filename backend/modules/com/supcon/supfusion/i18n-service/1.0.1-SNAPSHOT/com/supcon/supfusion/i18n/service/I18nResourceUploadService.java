package com.supcon.supfusion.i18n.service;

import com.supcon.supfusion.framework.cloud.common.result.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nResourceUploadService {

    Result judgePostModuleResource2(Map<String, String> map,String versionErrorSb);

    Result postModuleResource2(Map<String, String> filenameMap, List<String> moduleCodeList, List<String> moduleVersionList,Map<String, String> tokenMap) throws IOException;

    Result postModuleResourceOpenApi(Map map, String path);
}
