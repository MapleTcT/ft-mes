package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.entities.Script;
import com.supcon.supfusion.base.services.ScriptService;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class ScriptController extends ConfigurationBaseController {

    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/ec/scripts/manage")
    public String manage(ModelMap map, String entityCode) {
        map.put("entityCode", entityCode);
        return "scripts/manage";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/scripts/list")
    public Page list(String entityCode, Integer pageNo, Integer pageSize) {
        Page pager = new Page<Script>(pageNo, pageSize);
        return scriptService.find(entityCode, pager);
    }

    @RequestMapping(value = "/ec/scripts/edit")
    public String edit(ModelMap map, Long id, String entityCode) throws Exception {
        if (null != id) {
            map.put("script", scriptService.get(id));
        }
        map.put("entityCode", entityCode);
        return "scripts/edit";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/scripts/save")
    public Object save(@RequestParam(value="script.id", required=false) Long id,
                       @RequestParam(value="script.name", required=false) String name,
                       @RequestParam(value="script.scriptCode", required=false) String scriptCode,
                       @RequestParam(value="script.description", required=false) String description,
                       @RequestParam(value="script.code", required=false) String code,
                       @RequestParam(value="script.entityCode", required=false) String entityCode,
                       @RequestParam(value="script.version", required=false) Integer version) {
        if(id==null){
            boolean isExist = scriptService.hasScriptExist(entityCode, scriptCode);
            if(isExist){
                Map responseMap = new HashMap();
                responseMap.put("message", InternationalResource.get("script.scriptaction.select"));
                return responseMap;
            }
        }
        Script script = new Script();
        script.setId(id);
        script.setName(name);
        script.setEntityCode(entityCode);
        script.setScriptCode(scriptCode);
        script.setCode(code);
        script.setDescription(description);
        if (version == null) {
            script.setVersion(0);
        }else{
            script.setVersion(version+1);
        }
        scriptService.save(script);
        return script;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/scripts/delete")
    public Map delete(Long id) {
        Map responseMap = new HashMap<>();
        boolean isSuccess = true;
        if (null != id) {
            List<String> message = scriptService.hasScriptUsed(id);
            StringBuilder tempMessage = new StringBuilder();
            tempMessage.append(InternationalResource.get("foundation.script.delete.failed"));
            if(message.size()>0){
                for(String temp : message){
                    tempMessage.append("<li>"+temp+"</li>");
                }
                responseMap.put("message",tempMessage.toString());
                isSuccess = false;
            }else{
                isSuccess = scriptService.deleteWithCallback(id);
                responseMap.put("message",InternationalResource.get("foundation.infoSet.deleteSucess"));
            }
        }
        responseMap.put("isSuccess", isSuccess);
        return responseMap;
    }

    @RequestMapping(value = "/ec/scripts/select")
    public String select(ModelMap map, String entityCode,String callBackFuncName,Boolean multiSelect) {
        map.put("entityCode", entityCode);
        map.put("callBackFuncName",callBackFuncName);
        map.put("multiSelect",multiSelect);
        return "scripts/select";
    }

}
