package com.supcon.supfusion.custon.property.webapi.controller;

import com.supcon.supfusion.custon.property.common.enums.CoustomPropertyErrorEnum;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.common.exception.CoustomPropertyException;
import com.supcon.supfusion.custon.property.common.i18n.InternationalResource;
import com.supcon.supfusion.custon.property.dao.entity.*;
import com.supcon.supfusion.custon.property.server.EntityService;
import com.supcon.supfusion.custon.property.server.ModelServiceFoundation;
import com.supcon.supfusion.custon.property.server.ModuleServiceFoundation;
import com.supcon.supfusion.custon.property.server.ViewServiceFoundation;
import com.supcon.supfusion.custon.property.webapi.vo.response.PropertyResponseVO;
import com.supcon.supfusion.custon.property.webapi.vo.TreeRequestVO;
import com.supcon.supfusion.custon.property.webapi.vo.response.TreeResponseVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author zhang yafei
 */
@ResponseBody
@Api(tags = {"菜单树管理API"})

@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "${spring.application.name}")
@Slf4j
public class CustomPropertyTreeController {

    @Autowired
    private InternationalResource internationalResource;

    @Autowired
    private ModuleServiceFoundation moduleServiceFoundation;

    @Autowired
    private ModelServiceFoundation modelServiceFoundation;

    @Autowired
    private ViewServiceFoundation viewServiceFoundation;

    @Autowired
    private EntityService entityService;

    /**
     * @param treeRequestVO
     * @return
     */
    @ApiOperation(value = "加载菜单树")
    @GetMapping(value = "/tree")
    public ListResult<TreeResponseVO> CustomPropertyTree(
            @ApiParam(value = "菜单树对象", required = true) @Valid TreeRequestVO treeRequestVO
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<TreeResponseVO> treeList = new ArrayList<TreeResponseVO>();
        String code = treeRequestVO.getCode();
        switch (treeRequestVO.getType().toUpperCase()) {
            case "MODULE":
                List<Module> moduleList = moduleServiceFoundation.getAllModule();
                if (moduleList != null && moduleList.size() > 0) {
                    for (Module m : moduleList) {
                        if (!"sysbase_1.0".equals(m.getCode())) {
                            TreeResponseVO node = new TreeResponseVO();
                            node.setCode(m.getCode());
                            node.setName(internationalResource.getI18nValue(m.getName(), locale));
                            node.setIsParent(true);
                            treeList.add(node);
                        }
                    }
                }
                break;
            case "ENTITY":
                List<Entity> entityList = entityService.findEntities(code);
                if (entityList == null && entityList.size() <= 0) {
                    break;
                }
                for (Entity e : entityList) {
                    TreeResponseVO node = new TreeResponseVO();
                    node.setCode(e.getCode());
                    node.setName(internationalResource.getI18nValue(e.getName(), locale));
                    node.setIsParent(true);
                    treeList.add(node);
                }
                break;
            case "MODEL":

                List<Model> modelList = modelServiceFoundation.getModels(code);
                if (modelList != null && modelList.size() > 0) {
                    for (Model m : modelList) {
                        TreeResponseVO node = new TreeResponseVO();
                        node.setCode(m.getCode());
                        node.setName(internationalResource.getI18nValue(m.getName(), locale));
                        node.setIsParent(false);
                        treeList.add(node);
                    }
                }
                break;
            case "VIEW":
                List<View> viewList = viewServiceFoundation.findViews(code, ViewType.EDIT, ViewType.VIEW, ViewType.LIST, ViewType.EXTRA, ViewType.TREE, ViewType.REFERENCE);
                if (viewList != null && viewList.size() > 0) {
                    for (View view : viewList) {
                        TreeResponseVO node = new TreeResponseVO();
                        node.setCode(view.getCode());
                        node.setName(internationalResource.getI18nValue(view.getDisplayName(), locale) + "[" + view.getName() + "]");
                        node.setIsParent(false);
                        treeList.add(node);
                    }
                }

                break;
            default:
                throw new CoustomPropertyException(CoustomPropertyErrorEnum.LEVEL_UNKNOWN_TYPE_ERROR);

        }

        return new ListResult(treeList);
    }


    /**
     * @param moduleCode
     * @return
     */
    @ApiOperation(value = "通过模块code获取基础模块和相关联模块信息")
    @GetMapping(value = "/getRelateModuleByCode")
    public ListResult<TreeResponseVO> getRelateModuleByCod(
            @ApiParam(value = "模型code", required = true) @RequestParam(required = true) String moduleCode
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<Module> moduleList = new ArrayList<>();
        moduleList.add(moduleServiceFoundation.getModuleByCode("sysbase_1.0"));
        if (!"sysbase_1.0".equals(moduleCode)) {
            List<Module> relations = moduleServiceFoundation.getModuleRelaton(moduleCode);
            List<Module> referenceModules = moduleServiceFoundation.getReferences(moduleCode);
            if (relations != null && relations.size() > 0) {
                for (Module r : relations) {
                    moduleList.add(r);
                }
            }
            if (null != referenceModules && referenceModules.size() > 0) {
                moduleList.addAll(referenceModules);
            }
            Module moduleByCode = moduleServiceFoundation.getModuleByCode(moduleCode);
            if (moduleByCode != null) {
                moduleList.add(moduleByCode);
            }


        }
        ArrayList<TreeResponseVO> responseVOS = new ArrayList<>();
        moduleList.forEach(module -> {
            TreeResponseVO treeResponseVO = new TreeResponseVO();
            BeanUtils.copyProperties(module, treeResponseVO);
            treeResponseVO.setName(internationalResource.getI18nValue(module.getName(), locale));
            responseVOS.add(treeResponseVO);
        });
        return new ListResult(responseVOS);
    }


    /**
     * @param modelCode
     * @return
     */
    @ApiOperation(value = "通过模型code获取模型主键")
    @GetMapping(value = "/getPKProperty")
    public Result<PropertyResponseVO> getPKProperty(
            @ApiParam(value = "模型code", required = true) @RequestParam(required = true) String modelCode
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        Property property = modelServiceFoundation.getPKProperty(modelCode);
        PropertyResponseVO propertyResponseVO = new PropertyResponseVO();
        BeanUtils.copyProperties(property, propertyResponseVO);
        propertyResponseVO.setDisplayNameInternational(internationalResource.getI18nValue(property.getDisplayName(), locale));
        return new Result<>(propertyResponseVO);
    }

}
