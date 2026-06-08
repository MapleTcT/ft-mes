package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.enums.InfoSetDisplayType;
import com.supcon.supfusion.base.enums.InfoSetType;
import com.supcon.supfusion.base.services.CompanyService;
import com.supcon.supfusion.configuration.services.service.InfoSetService;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class InfoSetServiceImpl implements InfoSetService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SystemCodeService systemCodeService;

    @Override
    public Object[] getInfoSets(Long staffId, Long userId, Long companyId) {
        Long entityId = staffId == null ? 0l : staffId;

        long groupCompanyID=jdbcTemplate.queryForObject("SELECT ID FROM BASE_COMPANY WHERE TYPE='GROUP' and VALID=1", long.class);

        if (null != entityId) {
            String queryURL="SELECT * FROM BASE_INFOSET_TABLE WHERE ENTITY_CLASS = 'com.supcon.orchid.foundation.entities.Staff' AND VALID = 1 and (COMPANY_ID="+companyId+" or COMPANY_ID="+groupCompanyID+") ORDER BY SORT ,ID";
            //取出信息集表中的所有 tablename
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(queryURL);// 查出该Entity对应的所有的数据集表
            if (!tables.isEmpty()) {
                // 存在数据集

                Map<String, Object[]> stateCollection = new LinkedHashMap<String, Object[]>();// 状态集
                Map<String, Object[]> recordCollection = new LinkedHashMap<String, Object[]>();// 记录集
                Map<String, Map<String, String>> systemCodeCollection = new HashMap<String, Map<String, String>>();// 用到的系统编码

                Iterator<Map<String, Object>> tableIterator = tables.iterator();
                while (tableIterator.hasNext()) {
                    Map<String, Object> table = tableIterator.next();
                    //将 tables 中的每个tablename 取出
                    String name = (String) table.get("name");
                    //String intername=InternationalResource.get(name, getCurrentUser().getLanguage());
                    //name=intername;
                    table.put("NAME",name);
                    String targetTable = (String) table.get("TARGET_TABLE");
                    String targetFkName = (String) table.get("TARGET_FK_NAME");
                    String code = (String) table.get("CODE");
                    String queryColURL="SELECT * FROM BASE_INFOSET_COLUMN WHERE TABLE_CODE = '"+code+"' AND VALID = 1 AND DBFLAG=1 and (COMPANY_ID="+companyId+" or COMPANY_ID="+groupCompanyID+") ORDER BY SORT ,ID";
                    //根据CODE String code = (String) table.get("CODE") 和  登录公司的 iD 取得 每个 TABLE_CODE 中的字段的详细信息

                    List<Map<String, Object>> columns = jdbcTemplate.queryForList(queryColURL);


                    // ////////////////////////////////////////////////////////////////////////////////////////////
                    /* 取出没有权限的字段 */

                    List<Map<String, Object>> roleAccessColumns=new ArrayList<Map<String,Object>>();
                    if(userId!=null){
                        roleAccessColumns = jdbcTemplate.queryForList("SELECT * FROM BASE_INFOSET_USER WHERE USER_ID = ? AND INFOSET_TABLE_CODE = ?  and (COMPANY_ID=? or COMPANY_ID=?)", userId, code,companyId,groupCompanyID);
                    }

                    //log.debug("roleAccessColumns = {}", roleAccessColumns);
                    Map<String, Integer> userRoleMap = new HashMap<String, Integer>();

                    if (!roleAccessColumns.isEmpty()) {
                        for (Map<String, Object> roleCol : roleAccessColumns) {
                            Integer operateType = Integer.valueOf(roleCol.get("OPERATE_TYPE").toString());
                            userRoleMap.put((String) roleCol.get("INFOSET_COL_CODE"), operateType);
                        }
                    }

                    // ////////////////////////////////////////////////////////////////////////////////////////////

                    // 以下拿到的columns已经是有权限的了,至少是有读取权限
                    if (!columns.isEmpty()) {
                        log.debug("一个信息集.TYPE = {},NAME = {}", table.get("TYPE"), name);
                        StringBuilder sb = new StringBuilder();

                        for (Iterator<Map<String, Object>> it = columns.iterator(); it.hasNext();) {
                            Map<String, Object> col = it.next();

                            //  col.put("_editable", false);  这样在页面就根据  这个 将   readonly="true" 掉
                            if ((col.get("POWER_FLAG")==null||col.get("POWER_FLAG").toString().equals("0"))||(userRoleMap.get((String) col.get("CODE"))!=null&&userRoleMap.get((String) col.get("CODE")) == 2)) {
                                col.put("_editable", true);
                            } else if (userRoleMap.get((String) col.get("CODE"))!=null&&userRoleMap.get((String) col.get("CODE")) == 1) {
                                //如果记录中是VIEWPOWER  读 权限的 则
                                //  col.put("_editable", false);  这样在页面就根据这个判断  加上readonly="true"
                                col.put("_editable", false);
                            } else {
                                if((col.get("SYSTEMDEFAULT")!=null&&col.get("SYSTEMDEFAULT").toString().equals("1"))||(col.get("POWER_FLAG")!=null&&col.get("POWER_FLAG").toString().equals("0"))){
                                    col.put("_editable", true);
                                    //continue;
                                }else{
                                    //如果没有记录则 没有权限的 则it.remove()
                                    it.remove();
                                    continue;
                                }

                            }
                            String colname=(String)col.get("name");
                            if(colname!=null){
                                String intercolname=colname;
                                col.put("NAME",intercolname);
                            }

                            String colName = (String) col.get("CODE");
                            // col.put("CODE", colName.toUpperCase());
                            if (InfoSetDisplayType.valueOf((String) col.get("DISPLAY_TYPE")).equals(InfoSetDisplayType.SELECT) && null != col.get("SYSTEM_CODE_TYPE")) {
                                systemCodeCollection.put((String) col.get("CODE"), systemCodeService.getSystemCodeList((String) col.get("SYSTEM_CODE_TYPE"), false));
                            }

                            sb.append(",").append(colName);
                        }
                        if(!(targetTable.toUpperCase().equals("BASE_STAFF")||targetTable.toUpperCase().equals("BASE_LINKINFO"))){
                            sb.append(",ID");

                        }

                        if(sb.length()>0){
                            String lookupInfoSql = "SELECT " + sb.toString().substring(1) + " FROM " + targetTable + " WHERE " + targetFkName + " = ?";

                            if (InfoSetType.STATE.toString().equals(table.get("TYPE"))) {
                                // 状态型,1:1
                                Map<String, Object> stateMap = Collections.emptyMap();
                                try {
                                    stateMap = jdbcTemplate.queryForMap(lookupInfoSql, entityId);// 只会有一条
                                } catch (EmptyResultDataAccessException e) {
                                    // 没有数据
                                }
                                if (!columns.isEmpty()) {
                                    stateCollection.put(name, new Object[] { columns, stateMap, table });
                                }


                            } else {
                                // 记录型
                                List<Map<String, Object>> recordList = jdbcTemplate.queryForList(lookupInfoSql, entityId);// 记录集,有多条
                                if (!columns.isEmpty()) {
                                    recordCollection.put(name, new Object[] { columns, recordList, table });
                                }
                            }
                        }
                    }
                }
                return new Object[] { stateCollection, recordCollection, entityId, systemCodeCollection };

            }
        }
        return null;
    }
}
