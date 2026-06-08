package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.ScriptDaoImpl;
import com.supcon.supfusion.base.entities.Script;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.services.ScriptService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/27
 */
@Slf4j
@Service
@Transactional
public class ScriptServiceImpl implements ScriptService {

    @Autowired
    private ScriptDaoImpl scriptDao;
    @Autowired
    private InternationalService internationalService;

    @Override
    public Script get(long id) throws IOException {
        Script script = scriptDao.get(id);
        if (null != script) {
            File f = new File(getPath(script));
            if (f.exists()) {
                script.setCode(FileUtils.readFileToString(f, "UTF-8"));
            }
        }
        return script;
    }

    @Override
    public Page<Script> find(String entityCode, Page<Script> page) {
        scriptDao.findByPage(page, Restrictions.eq("entityCode", entityCode));
        return page;
    }

    @Override
    public Script get(String entityCode, String scriptCode) throws IOException {
        Script script = scriptDao.findEntityByHql("from Script where entityCode = ? and scriptCode = ?", (Object) entityCode, (Object) scriptCode);
        if (null != script) {
            File f = new File(getPath(script));
            if (f.exists()) {
                script.setCode(FileUtils.readFileToString(f, "UTF-8"));
            }
        }
        return script;
    }

    @Override
    public void save(Script script) {
        Assert.notNull(script);
        try {
            scriptDao.save(script);
            /* 保存code到文件，文件命名规则为 entityId/name */
            String path = getPath(script);
            File file = new File(path);
            FileUtils.writeStringToFile(file, script.getCode(), "UTF-8");
        } catch (ConstraintViolationException | IOException e) {
            throw new RuntimeException("脚本保存失败");
        }
    }

    @Override
    public boolean hasScriptExist(String entityCode, String scriptCode) {
        boolean isExist = false;
        String sqlQueryForScript = "select count(id) from SC_SCRIPT sc where sc.ENTITY_CODE = ? and sc.SCRIPT_CODE = ? ";
        Object scriptCount = scriptDao.createNativeQuery(sqlQueryForScript, new Object[]{entityCode,scriptCode}).uniqueResult();
        if(scriptCount!=null && !"0".equals(scriptCount.toString())){
            isExist = true;
        }
        return isExist;
    }

    @Value("${configuration-services.workspace:}")
    private String workspacePath;

    private String getPath(Script script) {
        File d = new File(workspacePath + File.separatorChar + "scripts" + File.separatorChar + script.getEntityCode());
        if (!d.exists()) {
            d.mkdirs();
        }
        return  workspacePath + File.separatorChar + "scripts" + File.separatorChar + script.getEntityCode() + File.separatorChar + script.getScriptCode();
    }

    @Override
    public List<String> hasScriptUsed(Long scriptId) {
        // TODO Auto-generated method stub
        boolean isUsed = true;
        List<String> messages = new ArrayList<String>();
        Script script = scriptDao.get(scriptId);
        String sqlQueryForView = "select code,name from ec_event e where e.entity_code = ? and e.event_function like ?";
        List<Object[]> views = scriptDao.createNativeQuery(sqlQueryForView, new Object[]{script.getEntityCode(),script.getScriptCode()}).list();
//        String sqlQueryForProView = "select code,name from project_event e where e.entity_code = ? and e.event_function like ?";
//        List<Object[]> proViews = scriptDao.createNativeQuery(sqlQueryForProView, new Object[]{script.getEntityCode(),script.getScriptCode()}).list();
        //String sqlQueryForTask = "select de.process_key,de.process_version,de.name from WF_TASK task left join WF_DEPLOYMENT de on task.DEPLOYMENT_ID = de.ID where de.ENTITY_CODE = ? and task.SCRIPT =?";
        String sqlQueryForTask = "SELECT de.process_key,de.process_version,de.name FROM WF_DEPLOYMENT de where de.ENTITY_CODE = ? and is_current_version =1 and  de.id in (select DISTINCT(task.DEPLOYMENT_ID)  from WF_TASK task where task.SCRIPT = ?)";
        List<Object[]> tasks = scriptDao.createNativeQuery(sqlQueryForTask, new Object[]{script.getEntityCode(),script.getScriptCode()}).list();
        //非工程期视图的检验
        if(views.size()>0){
            List<String> viewList = new ArrayList<String>();
            Map<String, Object> params = new HashMap<String, Object>();
            for(Object[] view : views){
                if(view!=null && view[0].toString()!=null && !"".equals(view[0].toString())&& view[1].toString()!=null && !"".equals(view[1].toString())){
                    StringBuilder viewCodeTemp = new StringBuilder();
                    String[] tempCode = view[0].toString().split("_");
                    for(int i = 0 ;i<4 ;i++){
                        viewCodeTemp.append("_"+tempCode[i]);
                    }
                    String getViewSqlForEc = "select NAME,DISPLAY_NAME from ec_view where code = ? and valid = 1";
                    List<Object[]> ecViews =scriptDao.createNativeQuery(getViewSqlForEc, viewCodeTemp.substring(1)).list();
                    if(ecViews.size()>0  ){
                        for(Object[] obj : ecViews){
                            messages.add(internationalService.getI18nValue("foundation.script.ec.view")+obj[0].toString()+" "+internationalService.getI18nValue("foundation.script.in")+view[1].toString()+internationalService.getI18nValue("foundation.script.used"));
                        }
                    }
                }
            }
        }

        if(tasks.size()>0){
            for(Object[] task : tasks){
                if(task!=null && task[0] !=null && !"".equals(task[0].toString()) && task[1] !=null && !"".equals(task[1].toString()) && task[2] !=null && !"".equals(task[2].toString())){
                    messages.add(internationalService.getI18nValue("foundation.script.workflow")+internationalService.getI18nValue(task[2].toString())+"("+task[0].toString()+")"+internationalService.getI18nValue("ec.property.is_fk"));
                }
            }
        }
        return messages;
    }

    @Override
    public boolean deleteWithCallback(long id) {
        Script script = null;
        boolean success = true;
        try {
            script = get(id);
            scriptDao.delete(script);
            //同时再删除workspace下对应的脚本
            String path = getPath(script);
            File file = new File(path);
            String modulePath = getModulePath(script);
            File moduleFile = new File(modulePath);
            if(file.exists()){
                file.delete();
            }
            if(moduleFile.exists()){
                moduleFile.delete();
            }
        } catch (IOException e) {
            success = false;
            throw new RuntimeException(e);
        }
        return success;
    }

    private String getModulePath(Script script) {
        String moduleCode = File.separatorChar+script.getEntityCode().substring(0,script.getEntityCode().lastIndexOf("_"));
        String path = workspacePath + File.separatorChar + "generate"+moduleCode+File.separatorChar+"scripts"+File.separatorChar+script.getEntityCode()+File.separatorChar + script.getScriptCode();
        File d = new File(path);
        if (!d.exists()) {
            return "";
        }
        return path;
    }
}
