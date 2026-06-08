package com.supcon.supfusion.i18n.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.i18n.dao.po.I18nTokenPO;

import java.util.List;

/**
 *
 * @Description: token表 自动上传同步自己应用服务国际化资源到国际化微服务端时控制用 DAO
 *
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
public interface I18nTokenDao extends BaseMapper<I18nTokenPO> {

    void add(I18nTokenPO tokenPO);

    I18nTokenPO selectByModuleCodeAndToken(I18nTokenPO i18nTokenPO);

    void deleteOne(String id);

    I18nTokenPO selectByModuleCodeAndValidOne(I18nTokenPO tokenPO);

    I18nTokenPO selectByModuleCodeAndValidZero(I18nTokenPO tokenPO);

    void deleteOneByModuleCodeAndToken(I18nTokenPO tokenPO2);

    void deleteByTokenId(Long s);

    List<I18nTokenPO> selectAllTokenPO();
}
