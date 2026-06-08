package com.supcon.supfusion.i18n.service;

import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.dao.vo.I18nLanguageVO;

/**
 *
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nInterApiService {

     PageResult<I18nLanguageVO> getAllLanguage();

     void updateI18nLanguage(List<Map<String, Object>> params);

     Result getI18nModuleVersionCode(List<String> list);

     Result postI18nModuleVersionCode(Map map);

     Result getI18nModuleIndexCode(String moduleCode);

     Result postI18nModuleIndexCode(String moduleCode);

}
