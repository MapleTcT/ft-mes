package com.supcon.supfusion.i18n.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 *
 * @Description:国际化键值对表Dao
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nResourceDao extends BaseMapper<I18nResourcePO> {

    int selectByKeyAndValueAndLanguageCount(Map whereMap);

    List<String> selectKeyByKeyAndValueAndLanguage(Map whereMap);

    List<I18nResourcePO> selectByKey(List<String> i18n_keys);

    List<I18nResourcePO> getAllNeedI18nKeyByModuleCodeAndLanguCode(I18nResourcePO i18nResourcePO);

    void updateByI18nKeyAndValidOne(I18nResourcePO i18nResourcePO);

    List<String> selectByKeys(Map<String, Object> whereMap);

    int selectByKeysCount(Map<String, Object> whereMap);

    List<String> selectKeysByKeys(Map<String, Object> whereMap);

    List<String> selectByOneKeyAndValues(Map<String, Object> whereMap);

    int selectByKeyOrValueCount(Map<String, Object> page);

    List<String> selectByKeyOrValue(Map<String, Object> page);

    void saveBatch(List<I18nResourcePO> i18n_pOs);

    int queryKeyByKeyCount(Map<String, Object> page);

    List<String> queryKeyByKey(Map<String, Object> page);

    int queryValueByValueCount(Map<String, Object> page);

    List<String> queryValueByValue(Map<String, Object> page);

    List<I18nResourcePO> selectByOneKey(I18nResourcePO i18nResPO);

    List<I18nResourcePO> queryObjectByKey(String key);

    void deleteByIdList(List<Long> ids);

    void deleteListByKeyAndLanguage(@Param("keys") List<String> keys, @Param("language")String language,@Param("moduleCode")String moduleCode);

    void deleteOneByKeyAndLanguCode(I18nResourcePO i18nResourcePO);

    void deleteOracleByModule(String moduleCode);

    List<I18nResourcePO> selectKeysByValuesReturnPO(Map<String, Object> whereMap);
}
