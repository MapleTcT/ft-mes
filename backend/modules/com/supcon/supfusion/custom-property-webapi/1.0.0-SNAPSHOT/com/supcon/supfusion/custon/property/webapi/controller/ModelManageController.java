package com.supcon.supfusion.custon.property.webapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.custon.property.common.i18n.InternationalResource;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import com.supcon.supfusion.custon.property.server.ModelServiceFoundation;
import com.supcon.supfusion.custon.property.server.ModuleServiceFoundation;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyModelBO;
import com.supcon.supfusion.custon.property.webapi.vo.AssociatedPropertyVO;
import com.supcon.supfusion.custon.property.webapi.vo.response.ModelMappingResponseVO;
import com.supcon.supfusion.custon.property.webapi.vo.ModelMappingVO;
import com.supcon.supfusion.custon.property.webapi.vo.PropertyVO;
import com.supcon.supfusion.custon.property.webapi.vo.ModelEnabledStatusVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author zhang yafei
 */
@ResponseBody
@Api(tags = {"模型管理API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "${spring.application.name}")
@Slf4j
public class ModelManageController {

    @Autowired
    private ModuleServiceFoundation moduleServiceFoundation;

    @Autowired
    private ModelServiceFoundation modelServiceFoundation;

    @Autowired
    private InternationalResource internationalResource;


    @ApiOperation(value = "查询模型下自定义字段")
    @GetMapping(value = "/modelManage/list")
    public ListResult<ModelMappingResponseVO> modelManageList(
            @ApiParam(value = "模型code", required = true) @RequestParam @NotBlank(message = "modelCode不能为空") String modelCode

    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<CustomPropertyModelBO> list = modelServiceFoundation.findCustomPropertyModelMappings(modelCode);

        List<ModelMappingResponseVO> collect = list.stream().map(customPropertyModelMapping -> {
            ModelMappingResponseVO modelMappingResponseVO = new ModelMappingResponseVO();
            BeanUtils.copyProperties(customPropertyModelMapping, modelMappingResponseVO);
            modelMappingResponseVO.setRefViewCode(customPropertyModelMapping.getReferenceViewCode());
            String fillContent = customPropertyModelMapping.getFillContent();
            if (StringUtils.isNotBlank(fillContent)){
                modelMappingResponseVO.setFillContent(JSONObject.parseObject(fillContent));
            }
            //设置字段信息
            PropertyVO propertyVO = new PropertyVO();
            Property property = customPropertyModelMapping.getProperty();
            BeanUtils.copyProperties(property, propertyVO);
            modelMappingResponseVO.setProperty(propertyVO);
            modelMappingResponseVO.setDisplayNameInternational(internationalResource.getI18nValue(customPropertyModelMapping.getDisplayName(),locale));
            modelMappingResponseVO.setModuleCode(property.getModuleCode());
            //设置关联字段信息
            Property associatedProperty = customPropertyModelMapping.getAssociatedProperty();
            if (associatedProperty != null){
                AssociatedPropertyVO associatedPropertyVO = new AssociatedPropertyVO();
                BeanUtils.copyProperties(associatedProperty,associatedPropertyVO);
                modelMappingResponseVO.setAssociatedProperty(associatedPropertyVO);
            }

            return modelMappingResponseVO;
        }).collect(Collectors.toList());
        return new ListResult(collect);
    }

    @ApiOperation(value = "保存接口")
    @PostMapping(value = "/modelManage/save")
    public Result<Object> modelManageSave( @RequestBody @Valid ModelMappingVO modelMappingVO) {
        CustomPropertyModelBO customPropertyModelMapping = new CustomPropertyModelBO();

        String fillContent = modelMappingVO.getFillContent() == null ? null : modelMappingVO.getFillContent().toJSONString();

        BeanUtils.copyProperties(modelMappingVO, customPropertyModelMapping);
        customPropertyModelMapping.setFillContent(fillContent);

        PropertyVO propertyVo = modelMappingVO.getProperty();
        Property property = new Property();
        BeanUtils.copyProperties(propertyVo, property);
        property.setFillcontent(fillContent);

        customPropertyModelMapping.setProperty(property);
        customPropertyModelMapping.setPropertyCode(property.getCode());
        customPropertyModelMapping.setReferenceViewCode(modelMappingVO.getRefViewCode());

        modelServiceFoundation.saveCustomPropertyModelMapping(customPropertyModelMapping);
        //刷新国际化数据
        internationalResource.refreshInternationalization();

        return new Result<>(HttpStatus.SC_OK,"succeed");
    }

    @ApiOperation(value = "批量修改运行状态")
    @PostMapping(value = "/modelManage/batcheUpdateEnabledStatus")
    public Result<Object>  batcheUpdateEnabledStatus( @RequestBody @Valid ModelEnabledStatusVO modelEnabledStatusVO) {
        modelServiceFoundation.enableProperty(modelEnabledStatusVO.getCodes(), modelEnabledStatusVO.getIds(), modelEnabledStatusVO.getEnabled());
        return new Result<>(HttpStatus.SC_OK,"succeed");
    }

    @ApiOperation(value = "排序接口")
    @PostMapping(value = "/modelManage/sort")
    public Result<Object> viewManageSort(  @RequestBody List<Long> ids) {
        if (!ids.isEmpty() || ids.size() > 0){
            modelServiceFoundation.modelManageSort(ids);
        }
        return new Result<>(HttpStatus.SC_OK, "succeed");
    }


}


