package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.dao.ActionViewDaoImpl;
import com.supcon.supfusion.configuration.services.service.ActionViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/27
 */
@ServiceApiService
@Transactional
public class ActionViewServiceImpl implements ActionViewService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ActionViewDaoImpl actionViewDao;

    @Override
    public void refreshModuleActionView(Module module, String... env) {
        String moduleCode = module.getCode();
        // 查询视图信息
        String viewSql = "SELECT CODE,NAME,URL,TYPE,SHOW_TYPE,IS_PRINT,ENTITY_CODE,ASS_MODEL_CODE FROM " + View.TABLE_NAME + " WHERE MODULE_CODE=? AND CUSTOM_FLAG=0";
        if (isRuntime(env)) {
            viewSql = "SELECT CODE,NAME,URL,TYPE,SHOW_TYPE,IS_PRINT,ENTITY_CODE,ASS_MODEL_CODE FROM RUNTIME_VIEW WHERE MODULE_CODE=? AND CUSTOM_FLAG=0";
        }
        List<Map<String, Object>> list = jdbcTemplate.queryForList(viewSql, moduleCode);

        // 查询实体所需信息
        String entitySql = "SELECT WORKFLOW_ENABLED,CODE FROM " + Entity.TABLE_NAME +" WHERE MODULE_CODE = ?";
        if (isRuntime(env)) {
            entitySql = "SELECT WORKFLOW_ENABLED,CODE FROM RUNTIME_ENTITY WHERE MODULE_CODE = ?";
        }
        List<Map<String, Object>> entityList = jdbcTemplate.queryForList(entitySql, moduleCode);

        // 查询主模型
        String modelSql = "SELECT CODE FROM " + Model.TABLE_NAME + " WHERE MODULE_CODE = ? AND IS_MAIN=1";
        if (isRuntime(env)) {
            modelSql = "SELECT CODE FROM RUNTIME_MODEL WHERE MODULE_CODE = ? AND IS_MAIN=1";
        }
        List<ActionView> actionViewList =new ArrayList<ActionView>();
        List<String> modelList = jdbcTemplate.queryForList(modelSql, new Object[] { moduleCode }, String.class);
        if (null != list && !list.isEmpty()) {
            jdbcTemplate.execute("DELETE FROM action_view WHERE VIEW_CODE LIKE '" + moduleCode + "_%'");
            for (Map<String, Object> map : list) {
                String entityCode = (null == map.get("ENTITY_CODE")) ? null : map.get("ENTITY_CODE").toString();
                String assModelCode = (null == map.get("ASS_MODEL_CODE")) ? null : map.get("ASS_MODEL_CODE").toString();
                boolean workflowEnabled = false;
                if (null != entityList && !entityList.isEmpty()) {
                    for (Map<String, Object> emap : entityList) {
                        if (emap.get("CODE").toString().equals(entityCode)) {
                            workflowEnabled = "1".equals(emap.get("WORKFLOW_ENABLED").toString());
                            break;
                        }
                    }
                }
                map.put("moduleType", module.getType());
                boolean isMainModel = (null != modelList && !modelList.isEmpty() && modelList.contains(assModelCode));

                this.refreshViewAction(map, workflowEnabled, isMainModel,actionViewList, env);
            }
        }
        if(actionViewList.size()>0){
            batchSaveActionView(actionViewList);
        }
    }

    @Override
    public void refreshSingleViewAction(View view, String... env) {
        String viewCode = view.getCode();
        String viewSql = "SELECT CODE,NAME,URL,TYPE,SHOW_TYPE,IS_PRINT,ENTITY_CODE,ASS_MODEL_CODE FROM " + View.TABLE_NAME + " WHERE CODE=? AND CUSTOM_FLAG=0";
        if (isRuntime(env)) {
            viewSql = "SELECT CODE,NAME,URL,TYPE,SHOW_TYPE,IS_PRINT,ENTITY_CODE,ASS_MODEL_CODE FROM RUNTIME_VIEW WHERE CODE=? AND CUSTOM_FLAG=0";
        }else if (isProj(env)) {
            viewSql = "SELECT CODE,NAME,URL,TYPE,SHOW_TYPE,IS_PRINT,ENTITY_CODE,ASS_MODEL_CODE FROM PROJECT_VIEW WHERE CODE=? AND CUSTOM_FLAG=0";
        }
        List<Map<String, Object>> list = jdbcTemplate.queryForList(viewSql, viewCode);
        if(null != list && !list.isEmpty()){
            Map<String,Object> map = list.get(0);
            map.put("moduleType", view.getEntity().getModule().getType());
            String entityCode = (null == map.get("ENTITY_CODE")) ? null : map.get("ENTITY_CODE").toString();
            String assModelCode = (null == map.get("ASS_MODEL_CODE")) ? null : map.get("ASS_MODEL_CODE").toString();
            // 查询实体所需信息
            String entitySql = "SELECT COUNT(1) FROM EC_ENTITY WHERE CODE = ? AND WORKFLOW_ENABLED=1";
            if (isRuntime(env)) {
                entitySql = "SELECT COUNT(1) FROM RUNTIME_ENTITY WHERE CODE = ? AND WORKFLOW_ENABLED=1";
            }
            Integer workflowResult = jdbcTemplate.queryForObject(entitySql, new String[]{entityCode}, Integer.class);

            // 查询主模型
            String modelSql = "SELECT COUNT(1) FROM " + Model.TABLE_NAME + " WHERE CODE = ? AND IS_MAIN=1";
            if (isRuntime(env)) {
                modelSql = "SELECT COUNT(1) FROM RUNTIME_MODEL WHERE CODE = ? AND IS_MAIN=1";
            }
            Integer modelResult = jdbcTemplate.queryForObject(modelSql, new String[]{assModelCode}, Integer.class);
            List<ActionView> actionViewList =new ArrayList<ActionView>();
            this.refreshViewAction(map, (workflowResult > 0), (modelResult > 0), actionViewList,env);
            if(actionViewList.size()>0){
                batchSaveActionView(actionViewList);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void deleteActionViewByViewcode(String viewCode) {
        actionViewDao.bulkExecute("delete ActionView where viewCode=?0", viewCode);
    }

    private void batchSaveActionView(final List<ActionView> actionViews){
        deleteActionView(actionViews);	//先删除已有的actionView
        String sql="insert INTO action_view(ACTION_URL,VIEW_CODE,VIEW_NAME) VALUES(?,?,?)";
        jdbcTemplate.batchUpdate(sql,  new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ActionView actionView=actionViews.get(i);
                ps.setString(1, actionView.getActionUrl());
                ps.setString(2, actionView.getViewCode());
                ps.setString(3, actionView.getViewName());
            }

            @Override
            public int getBatchSize() {
                return actionViews.size();
            }
        });
    }

    private void deleteActionView(final List<ActionView> actionViews){
        String sql = "DELETE FROM action_view where ACTION_URL in (:urls) ";
        String[] actionUrls = new String[actionViews.size()];
        int flag = 0;
        for(ActionView actionView : actionViews){
            actionUrls[flag] = actionView.getActionUrl();
            flag++;
        }
        actionViewDao.createNativeQuery(sql).setParameterList("urls", actionUrls).executeUpdate();
    }

    private void refreshViewAction(Map<String,Object> map,boolean workflowEnabled,boolean isMainModel,List<ActionView> actionViewList,String... env){
        String prefix = (null != map.get("URL") ? map.get("URL").toString().replaceAll("\\.action$", "") : null);
        if (null == prefix) {
            return;
        }else{
            //先临时这么处理，后续可把此前缀改为配置项
            if(prefix.startsWith("/msService")){
                prefix = prefix.replaceAll("/msService", "");
            }
        }

        String viewType = (null == map.get("TYPE")) ? null : map.get("TYPE").toString();
        String isPrint = (null == map.get("IS_PRINT")) ? null : map.get("IS_PRINT").toString();
        String viewName = (null == map.get("NAME")) ? null : map.get("NAME").toString();
        String viewCode = (null == map.get("CODE")) ? null : map.get("CODE").toString();
        String showType = (null == map.get("SHOW_TYPE")) ? null : map.get("SHOW_TYPE").toString();
        String entityCode = (null == map.get("ENTITY_CODE")) ? null : map.get("ENTITY_CODE").toString();
        String assModelCode = (null == map.get("ASS_MODEL_CODE")) ? null : map.get("ASS_MODEL_CODE").toString();
        String moduleType = (null == map.get("moduleType")) ? null : map.get("moduleType").toString();
        String suffix = ".action";
        if(null != moduleType && "Mis".equals(moduleType)){
            suffix = "";
        }
        if (null != viewType && !viewType.equals(ViewType.MNECODE.name())) {
            actionViewList.add(new ActionView(prefix + suffix, viewCode, viewName));
        }
        if (null != isPrint && Integer.valueOf(isPrint) > 0) {
            actionViewList.add(new ActionView(prefix + "Print" + suffix, viewCode, viewName));
        }
        if (null != showType && !showType.equals(ShowType.LAYOUT.name()) && !showType.equals(ShowType.LAYOUT2.name())) {
            if (null != viewType) {
                if (viewType.equals(ViewType.TREE.name()) || viewType.equals(ViewType.REFTREE.name())) {
                    actionViewList.add(new ActionView(prefix + "Drag" + suffix, viewCode, viewName));
                    actionViewList.add(new ActionView(prefix + "Sort" + suffix, viewCode, viewName));
                    actionViewList.add(new ActionView(prefix + "Data" + suffix, viewCode, viewName));
                    actionViewList.add(new ActionView(prefix + "FullData" + suffix, viewCode, viewName));

                    //this.saveActionView(new ActionView(prefix + "Drag.action", viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix + "Sort.action", viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix + "Data.action", viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix + "FullData.action", viewCode, viewName));
                } else if (viewType.equals(ViewType.LIST.name())) {
                    if (workflowEnabled && isMainModel) {
                        actionViewList.add(new ActionView(prefix + "-pending" + suffix, viewCode, viewName));
                        //this.saveActionView(new ActionView(prefix + "-pending.action", viewCode, viewName));
                    }
                    actionViewList.add(new ActionView(prefix + "-query" + suffix, viewCode, viewName));
                    actionViewList.add(new ActionView(prefix + "-getRequireData" + suffix, viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix + "-query.action", viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix + "-getRequireData.action", viewCode, viewName));
                } else if (viewType.equals(ViewType.REFERENCE.name())) {
                    if(ProjectFlagHolder.getInstance().getProjFlag().get()!=null&&ProjectFlagHolder.getInstance().getProjFlag().get()){
                        actionViewList.add(new ActionView(prefix.replace("/proj/","/projref/") + "-query" + suffix, viewCode, viewName));

                    }else{
                        actionViewList.add(new ActionView(prefix + "-query" + suffix, viewCode, viewName));
                    }
                    //this.saveActionView(new ActionView(prefix + "-query.action", viewCode, viewName));
                } else if (viewType.equals(ViewType.MNECODE.name())) {
                    actionViewList.add(new ActionView(prefix.replaceAll("[^/]+$", "") + "mneClient" + suffix, viewCode, viewName));
                    //this.saveActionView(new ActionView(prefix.replaceAll("[^/]+$", "") + "mneClient.action", viewCode, viewName));
                }
            }
        } else if (null != showType && showType.equals(ShowType.LAYOUT.name())) {
            String extVieSql = "SELECT * FROM EC_EXTRA_VIEW WHERE VIEW_CODE=?";
            if (isRuntime(env)) {
                extVieSql = "SELECT * FROM RUNTIME_EXTRA_VIEW WHERE VIEW_CODE=?";
            }
            ExtraView extraView = jdbcTemplate.queryForObject(extVieSql, new Object[] { viewCode }, new BeanPropertyRowMapper<ExtraView>(ExtraView.class));
            if (null != extraView && extraView.getConfigMap() != null) {
                Map<String, Object> layout = (Map<String, Object>) extraView.getConfigMap().get("layout");
                Set<String> keys = layout.keySet();
                for (String key : keys) {
                    Map<String, Object> item = (Map<String, Object>) layout.get(key);
                    if (item.get("ctype") != null && item.get("ctype").equals("tree") && item.get("id") != null) {
                        String layid = item.get("id").toString();
                        //this.saveActionView(new ActionView(prefix + "/tree/" + layid + ".action", viewCode + "_" + layid, viewName));
                        actionViewList.add(new ActionView(prefix + "/tree/" + layid + suffix, viewCode + "_" + layid, viewName));
                    }
                }
            }
        }
    }

    private boolean isRuntime(String... env) {
        return env == null || env.length == 0 || (!"ec".equals(env[0]) && !"proj".equals(env[0]));
    }
    private boolean isProj(String... env){
        return env == null || env.length == 0 || (!"ec".equals(env[0]) && !"runtime".equals(env[0]));
    }
}
