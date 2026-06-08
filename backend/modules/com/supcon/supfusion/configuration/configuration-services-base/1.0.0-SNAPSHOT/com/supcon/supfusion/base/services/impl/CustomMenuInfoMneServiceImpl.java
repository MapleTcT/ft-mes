package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.*;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.CustomMenuInfoMneService;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.framework.MneCodeGenterate;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Service("CustomMenuInfoMneService")
@Transactional
public class CustomMenuInfoMneServiceImpl implements CustomMenuInfoMneService {

    @Autowired
    private MenuInfoMnePODaoImpl menuInfoMnePODao;
    @Autowired
    private InternationalService internationalService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(List<MenuInfoPO> menuInfoPOS) {
        Locale locale = RpcContext.getContext().getLanguage();
        if (ObjectUtils.isEmpty(locale)) {
            RpcContext.getContext().setLanguage(Locale.SIMPLIFIED_CHINESE);
        }
        String language = RpcContext.getContext().getLanguage().toString();
        List<MenuInfoMneCodePO> menuInfoMneCodePOS = new ArrayList<>();
        menuInfoPOS.forEach(menuInfoPO -> {
            String i18nValue = internationalService.getI18nValue(menuInfoPO.getName());
            String deleteSql = "delete from rbac_menu_mnecode where MENU_INFO=?1";
            menuInfoMnePODao.createNativeQuery(deleteSql,menuInfoPO.getId()).executeUpdate();
            List<String> strings = MneCodeGenterate.mneCodeTupleGenerate(i18nValue);
            List<MenuInfoMneCodePO> mneCodePOS = strings.stream().map(s -> {
                MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
                menuInfoMneCodePO.setId(IDGenerator.newInstance().generate().longValue());
                menuInfoMneCodePO.setVersion("1");
                menuInfoMneCodePO.setLanguage(language);
                menuInfoMneCodePO.setMenuInfoId(menuInfoPO.getId());
                menuInfoMneCodePO.setMneCode(s);
                return menuInfoMneCodePO;
            }).collect(Collectors.toList());
            if (!ObjectUtils.isEmpty(mneCodePOS)){
                menuInfoMneCodePOS.addAll(mneCodePOS);
            }
            MenuInfoMneCodePO menuInfoMneCodePO = new MenuInfoMneCodePO();
            menuInfoMneCodePO.setId(IDGenerator.newInstance().generate().longValue());
            menuInfoMneCodePO.setVersion("1");
            menuInfoMneCodePO.setLanguage(language);
            menuInfoMneCodePO.setMenuInfoId(menuInfoPO.getId());
            menuInfoMneCodePO.setMneCode(i18nValue);
            menuInfoMneCodePOS.add(menuInfoMneCodePO);
        });
        String sql = "INSERT INTO rbac_menu_mnecode (ID, ROW_VERSION,LANGUAGE,MENU_INFO,MNE_CODE) "
                + "VALUES (?,?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MenuInfoMneCodePO menuInfoMneCodePO  = menuInfoMneCodePOS.get(i);
                if(null != menuInfoMneCodePO.getId()){
                    ps.setLong(1, menuInfoMneCodePO.getId());
                }else{
                    ps.setNull(1, Types.DECIMAL);
                }
                if(null != menuInfoMneCodePO.getVersion()){
                    ps.setString(2, menuInfoMneCodePO.getVersion());
                }else{
                    ps.setNull(2, Types.VARCHAR);
                }
                if(null != menuInfoMneCodePO.getLanguage()){
                    ps.setString(3, menuInfoMneCodePO.getLanguage());
                }else{
                    ps.setNull(3, Types.VARCHAR);
                }
                if(null != menuInfoMneCodePO.getMenuInfoId()){
                    ps.setLong(4,menuInfoMneCodePO.getMenuInfoId());
                }else{
                    ps.setNull(4, Types.DECIMAL);
                }
                if(null != menuInfoMneCodePO.getMneCode()){
                    ps.setString(5, menuInfoMneCodePO.getMneCode());
                }else{
                    ps.setNull(5, Types.VARCHAR);
                }
            }

            @Override
            public int getBatchSize() {
                return menuInfoMneCodePOS.size();
            }
        });
    }

}
