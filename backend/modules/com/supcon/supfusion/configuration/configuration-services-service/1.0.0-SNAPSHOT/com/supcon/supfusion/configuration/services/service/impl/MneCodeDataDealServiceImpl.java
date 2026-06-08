package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.service.MneCodeDataDealService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.utils.Inflector;
import com.supcon.supfusion.configuration.services.utils.MneCodeGenterate;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class MneCodeDataDealServiceImpl implements MneCodeDataDealService {

    @Autowired
    private ModelService modelService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void generateMneCodeData(Model model) {
        boolean flag = false;
        if(null != model) {
            String modelTableName = model.getTableName();
            String mcTableName =  DbUtils.getMneTable(model.getTableName());
            try {
                String testSql = "SELECT ID FROM " + modelTableName;
                List<Map<String, Object>> list = jdbcTemplate.queryForList(testSql);
                if(!list.isEmpty()) {
                    String mneSql = "SELECT ID FROM " + mcTableName;
                    List<Map<String, Object>> mneList = jdbcTemplate.queryForList(mneSql);
                    flag = true;
                } else {
                    flag = false;
                }
            } catch (Exception e) {
                flag = false;
            }
            if(flag) {
                String deleteSql = "DELETE FROM " + mcTableName;
                String insertSql = "INSERT INTO " + mcTableName + "(ID, MNE_CODE, " + Inflector.getInstance().columnize(StringUtils.firstLetterToLower(model.getModelName()))
                        + ", VERSION) VALUES (?,?,?,?)";
                if(model.getIsMneCode()) {
                    jdbcTemplate.update(deleteSql);
                    List<Property> propertyList = modelService.findProperties(model);
                    for(Property item : propertyList) {
                        if(item.getIsUsedMneCode() && (item.getType() == DbColumnType.TEXT || item.getType() == DbColumnType.BAPCODE || item.getType() == DbColumnType.SUMMARY)) {
                            String columnName = item.getColumnName();
                            String findSql = "SELECT ID AS id," + columnName + " AS mneCodeField FROM " + modelTableName;
                            List<Map<String, Object>> columnList = jdbcTemplate.queryForList(findSql);
                            for(Map<String, Object> map : columnList) {
                                if(null != map.get("mneCodeField") && map.get("mneCodeField").toString().length() > 0) {
                                    Set<String> mneCodeSet = new HashSet<String>();
                                    List<String> mneCodeList = MneCodeGenterate.mneCodeTupleGenerate(map.get("mneCodeField").toString().length()>10?map.get("mneCodeField").toString().substring(0, 10):map.get("mneCodeField").toString());
                                    if(null != mneCodeList && !mneCodeList.isEmpty()){
                                        mneCodeSet.addAll(mneCodeList);
                                    }
                                    mneCodeSet.add(((String) map.get("mneCodeField")).toLowerCase());
                                    for (String mneCode : mneCodeSet) {
                                        jdbcTemplate.update(insertSql, IDGenerator.newInstance().generate().longValue(), mneCode, map.get("id"), 0);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    jdbcTemplate.update(deleteSql);
                }
            }
        }
    }
}
