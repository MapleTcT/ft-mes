package com.supcon.supfusion.i18n.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.i18n.dao.po.I18nVersionPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description: 不同应用服务的国际化资源版本号表Dao
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nVersionDao extends BaseMapper<I18nVersionPO> {

    void add(I18nVersionPO versionPO);

    I18nVersionPO selectByModuleCode(@Param("moduleCode") String moduleCode);

    void deleteByModuleCode(@Param("moduleCode") String moduleCode);

    List<I18nVersionPO> selectAllVersionsByModuleCode(@Param("moduleCode") String moduleCode);
}
