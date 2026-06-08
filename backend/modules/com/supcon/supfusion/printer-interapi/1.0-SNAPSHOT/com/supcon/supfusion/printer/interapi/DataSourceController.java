package com.supcon.supfusion.printer.interapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.printer.common.Constants;
import com.supcon.supfusion.printer.common.PrinterCode;
import com.supcon.supfusion.printer.config.InternationalResource;
import com.supcon.supfusion.printer.interapi.vo.*;
import com.supcon.supfusion.printer.service.DataSourceService;
import com.supcon.supfusion.printer.service.bo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据源管理控制类
 * @author liyiming
 * @date 2020/10/9 5:01 下午
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "数据源管理", value = "数据源管理文档说明", hidden = true)
public class DataSourceController extends BaseController {

    @Autowired
    private DataSourceService dataSourceService;

    /**
     * 查询注册的对象实例数据源的带iframe的对象实例源的列表
     * @return
     */
    @GetMapping("/source/objects")
    @ResponseBody
    @ApiOperation(value = "查询注册的对象实例数据源的带iframe的对象实例源的列表", httpMethod = "GET")
    public ListResult<EntityPageUrlVO> listEntities() {
        List<EntityPageUrlBO> bos = dataSourceService.listEntites();
        if (bos == null || bos.size() == 0) {
            return new ListResult<>();
        }
        List<EntityPageUrlVO> vos = new ArrayList<>(bos.size());
        bos.stream().forEach(item -> {
            EntityPageUrlVO entityPageUrlVO = new EntityPageUrlVO();
            BeanUtils.copyProperties(item, entityPageUrlVO);
            vos.add(entityPageUrlVO);
        });
        return new ListResult<>(vos);
    }

    @PostMapping("/getEntityData")
    @ResponseBody
    @ApiOperation(value = "根据表单ID查询实体数据", httpMethod = "POST")
    public Result<List<EntityDataResultVO>> getEntityData(@RequestBody EntityQueryConditionVO entityQueryConditionVO) {
        EntityQueryConditionBO entityQueryConditionBO = new EntityQueryConditionBO();
        BeanUtils.copyProperties(entityQueryConditionVO, entityQueryConditionBO);
        if (entityQueryConditionVO.getCondition() != null) {
            ParamConditionBO paramConditionBO = new ParamConditionBO();
            BeanUtils.copyProperties(entityQueryConditionVO.getCondition(), paramConditionBO);
            entityQueryConditionBO.setCondition(paramConditionBO);
        }
        if (entityQueryConditionVO.getResultData() != null && entityQueryConditionVO.getResultData().size() > 0) {
            List<EntityConditionBO> resultDataBO = new ArrayList<>(entityQueryConditionVO.getResultData().size());
            entityQueryConditionVO.getResultData().stream().forEach(item -> {
                EntityConditionBO entityConditionBO = new EntityConditionBO();
                BeanUtils.copyProperties(item, entityConditionBO);
                resultDataBO.add(entityConditionBO);
            });
            entityQueryConditionBO.setResultData(resultDataBO);
        }
        List<EntityDataResultBO> dataResultBO = dataSourceService.getEntityData(entityQueryConditionBO);
        if (dataResultBO == null) {
            return new Result<>();
        }
        List<EntityDataResultVO> dataResultVOS = new ArrayList<>();
        dataResultBO.stream().forEach(item -> {
            EntityDataResultVO entityDataResultVO = new EntityDataResultVO();
            BeanUtils.copyProperties(item, entityDataResultVO);
            dataResultVOS.add(entityDataResultVO);
        });
        return new Result<>(dataResultVOS);
    }

    /**
     * 根据实体编码（app）查询模型列表（对象实例）
     * @param entityCode 实体编码
     * @return
     */
    @GetMapping("/entity/models")
    @ResponseBody
    @ApiOperation(value = "根据实体编码（app）查询模型列表", httpMethod = "GET")
    public Result<List<ModelVO>> getModelsByEntityCode(@ApiParam(value = "实体编码", required = true) @NotNull(message = Constants.PARAM_ENTITY_CODE_NECESSARY) @RequestParam(value = "entityCode", required = true) String entityCode) {
        List<ModelBO> bos = dataSourceService.getModelsByEntityCode(entityCode);
        if (bos == null || bos.size() == 0) {
            return new Result<>();
        }
        List<ModelVO> vos = new ArrayList<>();
        bos.stream().forEach(item -> {
            ModelVO modelVO = new ModelVO();
            BeanUtils.copyProperties(item, modelVO);
            vos.add(modelVO);
        });
        return new Result<>(vos);
    }

    /**
     * 模型列表动态加载子属性
     * @param modelCode
     * @param propertyCode
     * @return
     */
    @GetMapping("/model/properties")
    @ResponseBody
    @ApiOperation(value = "模型列表动态加载子属性", httpMethod = "GET")
    public Result<List<EntityModelVO>> getSubProperties(@ApiParam(value = "模型编码", required = true) @NotNull(message = Constants.PARAM_MODEL_CODE_NECESSARY) @RequestParam(value = "modelCode", required = true) String modelCode,
                                                      @ApiParam(value = "属性编码", required = true) @NotNull(message = Constants.PARAM_PROPERTY_CODE_NECESSARY) @RequestParam(value = "propertyCode", required = true) String propertyCode) {
        List<EntityModelBO> bos = dataSourceService.getSubProperties(modelCode, propertyCode);
        if (bos == null || bos.size() == 0) {
            return new Result<>();
        }
        List<EntityModelVO> vos = new ArrayList<>();
        bos.stream().forEach(item -> {
            EntityModelVO modelVO = new EntityModelVO();
            BeanUtils.copyProperties(item, modelVO);
            vos.add(modelVO);
        });
        return new Result<>(vos);
    }

    /**
     * 根据前端配置的http地址、参数，通过httpclient调用地址，返回数据
     * @param url
     * @param process
     * @return
     */
    @GetMapping("/service")
    @ApiOperation(value = "根据前端配置的http地址、参数，通过httpclient调用地址，返回数据", httpMethod = "GET")
    public Result<Object> urlRequest(@RequestParam(value = "url", required = true) String url,
                                     @RequestParam(value = "process", required = true) Integer process,
                                     @RequestHeader ("Accept-Language") String acceptLang) {
        Object result = dataSourceService.callCustomService(url, process);
        if (null == result) {
            return new Result<>(PrinterCode.CUSTOM_SERVICE_URL_INVALID.getCode(),
                    InternationalResource.get(PrinterCode.CUSTOM_SERVICE_URL_INVALID.getMessage(), acceptLang));
        }
        return new Result<>(result);
    }

}
