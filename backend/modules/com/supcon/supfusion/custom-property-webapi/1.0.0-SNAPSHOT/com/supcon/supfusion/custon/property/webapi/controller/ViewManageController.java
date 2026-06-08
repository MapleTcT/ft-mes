package com.supcon.supfusion.custon.property.webapi.controller;

import com.supcon.supfusion.custon.property.common.enums.AlignType;
import com.supcon.supfusion.custon.property.common.enums.CoustomPropertyErrorEnum;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.common.exception.CoustomPropertyException;
import com.supcon.supfusion.custon.property.common.i18n.InternationalResource;
import com.supcon.supfusion.custon.property.dao.entity.DataGrid;
import com.supcon.supfusion.custon.property.dao.entity.Property;
import com.supcon.supfusion.custon.property.dao.entity.View;
import com.supcon.supfusion.custon.property.server.SystemCodeService;
import com.supcon.supfusion.custon.property.server.ViewServiceFoundation;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyViewBO;
import com.supcon.supfusion.custon.property.server.bo.GroupSystemEntityBO;
import com.supcon.supfusion.custon.property.webapi.vo.PropertyVO;
import com.supcon.supfusion.custon.property.webapi.vo.ViewEnabledStatusVO;
import com.supcon.supfusion.custon.property.webapi.vo.response.DealSuccessResponseVO;
import com.supcon.supfusion.custon.property.webapi.vo.response.RefViewResponseVO;
import com.supcon.supfusion.custon.property.webapi.vo.response.ViewMappingResponseVO;
import com.supcon.supfusion.custon.property.webapi.vo.ViewMappingVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.xml.ws.Action;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhang yafei
 */
@ResponseBody
@Api(tags = {"视图管理API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "${spring.application.name}")
@Slf4j
public class ViewManageController {


    @Autowired
    private InternationalResource internationalResource;

    @Autowired
    private ViewServiceFoundation viewServiceFoundation;

    @Autowired
    SystemCodeService systemCodeService;

    @ApiOperation(value = "查询视图下自定义字段")
    @GetMapping(value = "/viewManage/list")
    public ListResult<ViewMappingResponseVO> viewManageList(
            @ApiParam(value = "视图code", required = true) @Valid @NotBlank(message = "viewCode不能为空") String viewCode
    ) {
        View view = viewServiceFoundation.getView(viewCode);
        if (view  == null){
            throw new CoustomPropertyException(CoustomPropertyErrorEnum.VIEW_DOES_NOT_ERROR);
        }
        List<CustomPropertyViewBO> customPropertyViewMappings = viewServiceFoundation.findCustomPropertyViewMappings(view, true);
        List<ViewMappingResponseVO> viewMappingVOS = getViewMappingResponseVOS(customPropertyViewMappings);
        return new ListResult<>(viewMappingVOS);
    }

    @ApiOperation(value = "分组查询视图下自定义字段")
    @GetMapping(value = "/ListByViewCode")
    @ResponseBody
    public Result<Object> listCustomPropertyOfView(
            @ApiParam(value = "视图code", required = true) @Valid @NotBlank(message = "viewCode不能为空") String viewCode) {
        View view = viewServiceFoundation.getView(viewCode);
        if (view  == null){
            throw new CoustomPropertyException(CoustomPropertyErrorEnum.VIEW_DOES_NOT_ERROR);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        //视图的自定义字段
        List<CustomPropertyViewBO> customPropertyViewMappings = viewServiceFoundation.findCustomPropertyViewMappings(view, false);
        Map<String, Object> viewCustomPropertyMap = new HashMap<>();
        List<ViewMappingResponseVO> viewMappingResponseVOS = getViewMappingResponseVOS(customPropertyViewMappings);
        if (viewMappingResponseVOS !=null && viewMappingResponseVOS.size() > 0){
            viewCustomPropertyMap.put(viewCode, viewMappingResponseVOS.get(0).getList());
            result.add(viewCustomPropertyMap);
        }

        //datagrid的自定义字段
        List<DataGrid> dataGrids = viewServiceFoundation.getDataGrids(viewCode);
        if (dataGrids != null) {
            for (DataGrid datagrid : dataGrids) {
                String datagridCode = datagrid.getCode();
                DataGrid dg = viewServiceFoundation.getDataGrid(datagridCode);
                List<CustomPropertyViewBO> dgCustomPropertys = viewServiceFoundation.findDgCustomePropertyByDgCode(dg,view.getType());
                Map<String, Object> dgCustomPropertyMap = new HashMap<>();
                dgCustomPropertyMap.put(datagridCode, getViewMappingResponseVOS(dgCustomPropertys));
                result.add(dgCustomPropertyMap);
            }
        }
        return new Result<Object>(HttpStatus.SC_OK,"true",result);
    }

    @ApiOperation(value = "开启或者隐藏自定义字段")
    @PostMapping(value = "/viewManage/save")
    @ResponseBody
    public Result<Object> viewManageSave(
            @ApiParam(value = "视图code", required = true) @Valid @RequestBody ViewMappingVO viewMappingVO
    ) {
        if (viewMappingVO.getColspan() != null && viewMappingVO.getColspan() > viewMappingVO.getTextareaRow()){
            throw new CoustomPropertyException(CoustomPropertyErrorEnum.VCOLUMN_NUMBER_ERROR);
        }
        CustomPropertyViewBO customPropertyViewMapping = new CustomPropertyViewBO();
        BeanUtils.copyProperties(viewMappingVO, customPropertyViewMapping);
        Property property = new Property();
        BeanUtils.copyProperties(viewMappingVO.getProperty(), property);
        customPropertyViewMapping.setProperty(property);
        if (customPropertyViewMapping.getPropertyLayRec() == null || customPropertyViewMapping.getPropertyLayRec().length() < 1) {
            customPropertyViewMapping.setPropertyLayRec(null);
        }
        viewServiceFoundation.saveCustomPropertyViewMapping(customPropertyViewMapping);
        //刷新国际化数据
        internationalResource.refreshInternationalization();
        return new Result<>(HttpStatus.SC_OK, "succeed");
    }


    @ApiOperation(value = "批量开启或者隐藏自定义字段")
    @PostMapping(value = "/viewManage/showOrHidden")
    public Result<Object> viewManageShowOrHidden(
            @ApiParam(value = "视图code", required = true) @Valid @RequestBody ViewEnabledStatusVO viewEnabledStatusVO
            ) {
        viewServiceFoundation.showProperty(viewEnabledStatusVO.getCodes(), viewEnabledStatusVO.getIds(), viewEnabledStatusVO.getEnabled());
        return new Result<>(HttpStatus.SC_OK, "succeed");
    }

    @ApiOperation(value = "排序接口")
    @PostMapping(value = "/viewManage/sort")
    public Result<Object> viewManageSort(  @RequestBody List<Long> ids) {
        if (!ids.isEmpty() || ids.size() > 0){
            viewServiceFoundation.viewManageSort(ids);
        }

        return new Result<>(HttpStatus.SC_OK, "succeed");
    }


    @ApiOperation(value = "获取参照视图")
    @GetMapping(value = "/viewManage/refViews")
    public ListResult<RefViewResponseVO> refViews(
            @ApiParam(value = "视图code", required = true)  @RequestParam(required = true) String  modelCode
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<View> refViews = viewServiceFoundation.findViewsByModelCode(modelCode, ViewType.REFERENCE);
        ArrayList<RefViewResponseVO> refViewResponseVOS = new ArrayList<>();
        refViews.forEach(refView ->{
            RefViewResponseVO refViewResponseVO = new RefViewResponseVO();
            BeanUtils.copyProperties(refView,refViewResponseVO);
            refViewResponseVO.setDisplayNameInternational(internationalResource.getI18nValue(refView.getDisplayName(),locale));
            refViewResponseVOS.add(refViewResponseVO);
        });
        return new ListResult<>(refViewResponseVOS);
    }

    @ApiOperation(value = "获取系统编码")
    @GetMapping(value = "/viewManage/systemCode")
    public ListResult<GroupSystemEntityBO> systemCodeMap(
            @ApiParam(value = "模型code", required = true)  @RequestParam(required = true) String  moduleCode
    ) {
        List<GroupSystemEntityBO> groupSystemEntityBOS= systemCodeService.getSystemEntityMapByGroup(moduleCode);
        return new ListResult<>(groupSystemEntityBOS);
    }

    /**
     * DataGrid中名称显示关联关系
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/foundation/customProp/getPropDisplayName")
    public Result<DealSuccessResponseVO> getPropDisplayName(
            @ApiParam(value = "模型code", required = true)  @RequestParam(required = true) String  modelCode,
            @ApiParam(value = "propLayRec", required = true)  @RequestParam(required = true) String  propLayRec
    )  {
        DealSuccessResponseVO dealSuccessResponseVO = new DealSuccessResponseVO();
        dealSuccessResponseVO.setDealSuccessFlag(Boolean.TRUE);
        dealSuccessResponseVO.setPropDisplayName(viewServiceFoundation.findPropDisplayName(propLayRec, modelCode));
        return new Result<>(dealSuccessResponseVO);
    }


    /**
     * 转VO
     *
     * @param customPropertyViewMappings
     * @return
     */
    private List<ViewMappingResponseVO> getViewMappingResponseVOS(List<CustomPropertyViewBO> customPropertyViewMappings) {
        Locale locale = LocaleContextHolder.getLocale();
        return customPropertyViewMappings.stream().map(customPropertyViewMapping -> {
            ViewMappingResponseVO viewMappingResponseVO = new ViewMappingResponseVO();
            BeanUtils.copyProperties(customPropertyViewMapping, viewMappingResponseVO);

            Property property = customPropertyViewMapping.getProperty();
            if (property != null) {
                PropertyVO propertyVO = new PropertyVO();
                BeanUtils.copyProperties(property, propertyVO);
                viewMappingResponseVO.setProperty(propertyVO);
            }

            viewMappingResponseVO.setCode(customPropertyViewMapping.get_code());
            viewMappingResponseVO.setParentCode(customPropertyViewMapping.get_parentCode());
            List<CustomPropertyViewBO> list = customPropertyViewMapping.getList();
            if (list != null && list.size() > 0) {
                viewMappingResponseVO.setList(getViewMappingResponseVOS(list));
            }
            viewMappingResponseVO.setDisplayNameInternational(internationalResource.getI18nValue(customPropertyViewMapping.getDisplayName(),locale));
            return viewMappingResponseVO;
        }).collect(Collectors.toList());
    }

}
