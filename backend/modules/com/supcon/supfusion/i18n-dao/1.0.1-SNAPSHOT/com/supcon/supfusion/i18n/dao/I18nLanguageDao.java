package com.supcon.supfusion.i18n.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;

/**
 *
 * @Description: 国际化key对应的语言类型表Dao
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nLanguageDao  extends BaseMapper<I18nLanguagePO> {

    void updateByLanguageCode(I18nLanguagePO i18nLanguagePO);


}
