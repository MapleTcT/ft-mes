package com.supcon.supfusion.i18n.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 每个模块的国际化资源索引表Dao
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nIndexDao extends BaseMapper<I18nIndexPO> {

    I18nIndexPO selectByModuleCode(@Param("moduleCode")String moduleCode);

    void add(I18nIndexPO i18nIndexPO);

    void deleteByModuleCode(@Param("moduleCode")String moduleCode);

    void updateByModuleIndexCode(I18nIndexPO i18nIndexPO);

    List<I18nIndexPO> queryAllModuleCode();
}
