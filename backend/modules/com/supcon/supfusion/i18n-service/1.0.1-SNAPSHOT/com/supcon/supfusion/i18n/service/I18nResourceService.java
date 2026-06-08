package com.supcon.supfusion.i18n.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.dto.I18nQueryDTO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.dao.vo.ExcelVO;
import com.supcon.supfusion.i18n.dao.vo.I18nResourceVO;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nResourceService {
	

    PageResult getAllModel();

    PageResult createNewInternationalByValue(String custom, String moduleCode);

    PageResult addI18nResource(Map map);

    PageResult updateI18nResource(Map map);

    PageResult deleteI18nResourceByKey(String i18n_key);

    PageResult exportExcelModel();

    Result importation(String originalFileName, String uuidFilename) throws IOException;

    Result<ExcelVO> checkImportStatus(long id);

    PageResult<I18nResourceVO> queryByMap(I18nQueryDTO queryDto, Pagination pagination);

    Result exportExcelFile(List<String> i18nKeysList, I18nQueryDTO queryDto, Pagination pagination, boolean downloadAll);

    PageResult<I18nResourceVO> queryByKeyOrValue(String i18nKey, String fuzzyI18nValue, int pageNo, int pageSize);

    PageResult<String> queryKeyOrValueByKeyOrValue(String i18nKey, String i18nValue, String language, int pageNo, int pageSize);

    List<I18nResourceVO> queryKeyExist(String key);
    
    List<I18nResourceVO> getValueByKey(String key);

    String queryOneByOneKey(String i18nKey);

    Map<String, String> queryKeyValuesByKeys(String[] keysArr);

    Map<String, String> queryKeyValuesByKeys1(String[] keysArr);

    Result<String> searchOneI18NKeyValues(String key, String language);

    Result searchI18NKeysValues(List<String> list, String language);

    I18nResourceVO queryByI18nKey(String key);
    
    List<I18nLanguagePO> getAllLanguage(String tenantId);
    
    List<String> getEnableLanguage(String tenantId);
    
    void saveToDB(String moduleCode, String newVersionCode, boolean hasResource, List<I18nResourcePO> i18nResourceList);

    Map<String, String> putResourceToCache(String moduleCode, String language);
}
