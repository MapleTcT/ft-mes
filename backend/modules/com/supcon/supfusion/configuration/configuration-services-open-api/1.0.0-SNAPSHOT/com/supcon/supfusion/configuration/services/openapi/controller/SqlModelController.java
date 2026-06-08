package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.SqlModelService;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import java.util.*;


@Slf4j
@Controller
@RequestMapping("/ec/sqlmodel")
public class SqlModelController extends ConfigurationBaseController {

    @Autowired
    private ModelService modelService;
    @Autowired
    private SqlModelService sqlModelService;
    @Autowired
    private EntityService entityService;

    @RequestMapping("/edit")
    public String edit(ModelMap map, @RequestParam("entity.code") String entityCode,
                       @Nullable @RequestParam("model.code") String modelCode) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        Map<String, Object> responseMap = new HashMap<String, Object>();
        if (!StringUtils.isEmpty(modelCode)) {
            SqlModel sqlModel = sqlModelService.getSqlModel(modelCode);
            Model model = modelService.getModel(modelCode);
            // 兼容老版本
            if (sqlModel == null) {
                sqlModel = new SqlModel();
                sqlModel.setModelSql(model.getSql());
            }
            model.setSqlModel(sqlModel);
            model.setEntity(entity);
            map.addAttribute("model", model);
        } else {
            responseMap.put("firstIsMain", modelService.firstIsMain(entity));
        }
        map.addAttribute("entity", entity);
        map.addAttribute("module", entity.getModule());
        map.addAttribute("responseMap", responseMap);
        return "model/editsql";
    }

    @ResponseBody
    @RequestMapping(value = "/save")
    public ResponseMsg save() throws Exception {
        Model model = DtoUtils.getModelVO(getRequest());
        sqlModelService.addSqlModel(model);
        ResponseMsg response = new ResponseMsg(true);
        return response;
    }

    @ResponseBody
    @RequestMapping(value = "/checkSqlModel")
    public ResponseMsg checkSqlModel() {
        Model model = DtoUtils.getModelVO(getRequest());
        sqlModelService.checkSqlModel(model);
        return new ResponseMsg(true);
    }

    /**
     * SQL模型根据SQL获取字段信息
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/editSqlProperties")
    public String editSqlProperties(ModelMap map) {
        Model model = DtoUtils.getModelVO(getRequest());
        List<Property> properties = sqlModelService.getProperties(model);
        Entity entity = entityService.getEntity(model.getEntity().getCode());
        map.addAttribute("properties", properties);
        map.addAttribute("model", model);
        map.addAttribute("module", entity.getModule());
        return "model/editsqlproperties";
    }

}
