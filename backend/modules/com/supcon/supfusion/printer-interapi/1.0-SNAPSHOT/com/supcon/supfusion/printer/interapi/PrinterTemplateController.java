package com.supcon.supfusion.printer.interapi;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.printer.common.PrinterCode;
import com.supcon.supfusion.printer.config.InternationalResource;
import com.supcon.supfusion.printer.interapi.vo.*;
import com.supcon.supfusion.printer.service.PrinterLabelService;
import com.supcon.supfusion.printer.service.PrinterTemplateService;
import com.supcon.supfusion.printer.service.bo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印模板控制类
 * @author liyiming
 * @date 2020/10/9 3:19 下午
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "打印模板管理", value = "打印模板管理文档说明", hidden = true)
public class PrinterTemplateController extends BaseController {
    @Autowired
    private PrinterTemplateService printerTemplateService;
    @Autowired
    private PrinterLabelService printerLabelService;
    @Autowired
    private MessageResourceService messageResourceService;

    /**
     * 新增打印模板
     * @param printerTemplateAddVO
     * @return
     */
    @PostMapping("/template")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="创建打印模板接口", httpMethod="POST")
    public Result<Long> doCreate(@RequestBody @Validated PrinterTemplateAddVO printerTemplateAddVO, @RequestHeader ("Accept-Language") String acceptLang) {
        Integer count = printerTemplateService.templateCodeCount(printerTemplateAddVO.getTemplateCode());
        if(count > 0){
            return new Result<>(PrinterCode.TEMPLATE_CODE_UNIQUE_ERROR.getCode(), InternationalResource.get(PrinterCode.TEMPLATE_CODE_UNIQUE_ERROR.getMessage(), acceptLang));
        }
        PrinterTemplateAddBO printerTemplateAddBO = new PrinterTemplateAddBO();
        BeanUtils.copyProperties(printerTemplateAddVO, printerTemplateAddBO);

        return new Result<>(printerTemplateService.addPrinterTemplate(printerTemplateAddBO));
    }

    /**
     * 批量删除打印模板
     * @param templateIds
     * @return
     */
    @DeleteMapping("/templates/{templateIds}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="批量删除打印模板接口", httpMethod="DELETE")
    public Result<?> doDelete(@PathVariable("templateIds") String templateIds) {
        String[] templateIdArr = templateIds.split(",");
        List<Long> templateIdList = new ArrayList<>();
        for(int i=0; i<templateIdArr.length; i++){
            templateIdList.add(Long.parseLong(templateIdArr[i]));
        }
        printerTemplateService.deleteBatchPrinterTemplates(templateIdList);
        return new Result<>();
    }

    /**
     * 编辑打印模板
     * @param printerTemplateUpdateVO
     * @return
     */
    @PutMapping("/template")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="编辑打印模板接口", httpMethod="PUT")
    public Result<?> doEditor(@RequestBody @Validated PrinterTemplateUpdateVO printerTemplateUpdateVO) {
        PrinterTemplateUpdateBO printerTemplateUpdateBO = new PrinterTemplateUpdateBO();
        BeanUtils.copyProperties(printerTemplateUpdateVO, printerTemplateUpdateBO);
        if(!CollectionUtils.isEmpty(printerTemplateUpdateVO.getPageDatas())){
            List<PrinterTemplateRelationPageBO> printerTemplateRelationPageBOList = new ArrayList<>();
            printerTemplateUpdateVO.getPageDatas().forEach(p -> {
                PrinterTemplateRelationPageBO printerTemplateRelationPageBO = new PrinterTemplateRelationPageBO();
                BeanUtils.copyProperties(p, printerTemplateRelationPageBO);
                printerTemplateRelationPageBOList.add(printerTemplateRelationPageBO);
            });
            printerTemplateUpdateBO.setPageDatas(printerTemplateRelationPageBOList);
        }
        printerTemplateService.updatePrinterTemplate(printerTemplateUpdateBO);
        return new Result<>();
    }

    @PutMapping("/template/batch")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "批量修改状态", httpMethod = "PUT")
    public Result<?> doBatchEditor(@RequestBody @Validated PrinterTemplateBatchUpdateVO printerTemplateBatchUpdateVO) {
        PrinterTemplateBatchUpdateBO printerTemplateBatchUpdateBO = new PrinterTemplateBatchUpdateBO();
        BeanUtils.copyProperties(printerTemplateBatchUpdateVO, printerTemplateBatchUpdateBO);
        printerTemplateService.batchUpdateTemplateStatus(printerTemplateBatchUpdateBO);
        return new Result<>();
    }

    /**
     * 复制打印模板
     * @param printerTemplateUpdateVO
     * @return
     */
    @PutMapping("/template/copy")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="复制打印模板接口", httpMethod="PUT")
    public Result<?> doCopy(@RequestBody @Validated PrinterTemplateUpdateVO printerTemplateUpdateVO,@RequestHeader ("Accept-Language") String acceptLang) {
        Integer count = printerTemplateService.templateCodeCount(printerTemplateUpdateVO.getTemplateCode());
        if(count > 0){
            return new Result<>(PrinterCode.TEMPLATE_CODE_UNIQUE_ERROR.getCode(), InternationalResource.get(PrinterCode.TEMPLATE_CODE_UNIQUE_ERROR.getMessage(), acceptLang));
        }
        PrinterTemplateUpdateBO printerTemplateUpdateBO = new PrinterTemplateUpdateBO();
        BeanUtils.copyProperties(printerTemplateUpdateVO, printerTemplateUpdateBO);
        if(!CollectionUtils.isEmpty(printerTemplateUpdateVO.getPageDatas())){
            List<PrinterTemplateRelationPageBO> printerTemplateRelationPageBOList = new ArrayList<>();
            printerTemplateUpdateVO.getPageDatas().forEach(p -> {
                PrinterTemplateRelationPageBO printerTemplateRelationPageBO = new PrinterTemplateRelationPageBO();
                BeanUtils.copyProperties(p, printerTemplateRelationPageBO);
                printerTemplateRelationPageBOList.add(printerTemplateRelationPageBO);
            });
            printerTemplateUpdateBO.setPageDatas(printerTemplateRelationPageBOList);
        }
        printerTemplateService.copyPrinterTemplate(printerTemplateUpdateBO);
        return new Result<>();
    }

    /**
     * 获取打印膜版
     * @param templateId
     * @return
     */
    @GetMapping("/template/{templateId}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="获取打印模板接口", httpMethod="GET")
    public Result<?> doGet(@PathVariable("templateId") Long templateId,@RequestHeader ("Accept-Language") String acceptLang) {
        PrinterTemplateUpdateBO printerTemplateUpdateBO = printerTemplateService.queryPrinterTemplateListByTemplateId(templateId);
        PrinterTemplateUpdateVO printerTemplateUpdateVO = new PrinterTemplateUpdateVO();
        BeanUtils.copyProperties(printerTemplateUpdateBO, printerTemplateUpdateVO);
        String templateName = InternationalResource.get(printerTemplateUpdateVO.getI18nKey(), acceptLang);
        printerTemplateUpdateVO.setTemplateName(templateName);
        List<PrinterTemplateRelationPageBO> printerTemplateRelationPageBOList = printerTemplateUpdateBO.getPageDatas();
        List<PrinterTemplateRelationPageVO> printerTemplateRelationPageVOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(printerTemplateRelationPageBOList)){
            printerTemplateRelationPageBOList.forEach(p -> {
                PrinterTemplateRelationPageVO printerTemplateRelationPageVO = new PrinterTemplateRelationPageVO();
                BeanUtils.copyProperties(p, printerTemplateRelationPageVO);
                printerTemplateRelationPageVOList.add(printerTemplateRelationPageVO);
            });
        }
        printerTemplateUpdateVO.setPageDatas(printerTemplateRelationPageVOList);
        return new Result<>(printerTemplateUpdateVO);
    }

    /**
     * 获取打印模板列表
     * @return
     */
    @GetMapping("/templates/{appId}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="获取打印模板列表接口", httpMethod="GET")
    public PageResult<?> doList(@PathVariable("appId") String appId,@RequestParam(value = "pageSize", required = false) Integer pageSize, @RequestParam(value = "current", required = false) Integer current, @RequestHeader ("Accept-Language") String acceptLang) {
        PrinterTemplatePageQueryBO printerTemplatePageQueryBO = new PrinterTemplatePageQueryBO();
        printerTemplatePageQueryBO.setAppId(appId);
        printerTemplatePageQueryBO.setPageNum(current);
        printerTemplatePageQueryBO.setPageSize(pageSize);
        Page page = printerTemplateService.queryPrinterTemplateListByAppId(printerTemplatePageQueryBO);
        List<PrinterTemplateUpdateBO> printerTemplateUpdateBOList = page.getRecords();
        List<PrinterTemplateUpdateVO>  printerTemplateUpdateVOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(printerTemplateUpdateBOList)){
            printerTemplateUpdateBOList.forEach(p -> {
                PrinterTemplateUpdateVO printerTemplateUpdateVO = new PrinterTemplateUpdateVO();
                BeanUtils.copyProperties(p, printerTemplateUpdateVO);
                String templateName = InternationalResource.get(printerTemplateUpdateVO.getI18nKey(), acceptLang);
                printerTemplateUpdateVO.setTemplateName(templateName);
                printerTemplateUpdateVOList.add(printerTemplateUpdateVO);
            });
        }
        return new PageResult<PrinterTemplateUpdateVO>(printerTemplateUpdateVOList, page.getTotal(), pageSize, current);
    }

    /**
     * 保存标签
     * @return
     */
    @PostMapping("/label")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="保存标签", httpMethod="POST")
    public Result<?> doCreateLabel(@RequestBody PrinterLabelVO printerLabelVO) {
        PrinterLabelBO printerLabelBO = new PrinterLabelBO();
        BeanUtils.copyProperties(printerLabelVO, printerLabelBO);
        printerLabelService.addPrinterLabel(printerLabelBO);
        return new Result<>();
    }

    /**
     * 获取打印标签列表
     * @return
     */
    @GetMapping("/labels")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="获取打印标签列表接口", httpMethod="GET")
    public Result<?> doLabelList() {
        List<PrinterLabelBO> printerLabelBOList = printerLabelService.queryPrinterLabelList();
        List<PrinterLabelVO> printerLabelVOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(printerLabelBOList)){
            printerLabelBOList.forEach(p -> {
                PrinterLabelVO printerLabelVO = new PrinterLabelVO();
                BeanUtils.copyProperties(p,printerLabelVO);
                printerLabelVOList.add(printerLabelVO);
            });
        }
        return new Result<>(printerLabelVOList);
    }

    /**
     * 设计模板json内容保存
     * @param printerDesignContentVO 设计模板内容
     */
    @PostMapping("/templateDesign")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "保存设计模板json内容", httpMethod = "POST")
    public void saveTemplateDesignContent(@RequestBody(required = true) PrinterDesignContentVO printerDesignContentVO) throws IOException {
        PrinterDesignContentBO printerDesignContentBO = new PrinterDesignContentBO();
        BeanUtils.copyProperties(printerDesignContentVO, printerDesignContentBO);
        printerTemplateService.saveTemplateDesignContent(printerDesignContentBO);
    }

    /**
     * 加载设计模板的json内容
     * @param templateId 打印模板id
     * @return
     */
    @GetMapping("/{templateId}/templateDesign")
    @ApiOperation(value = "加载设计模板json内容", httpMethod = "GET")
    public Result<PrinterDesignContentVO> loadTemplateDesignContent(@PathVariable(value = "templateId") Long templateId) throws UnsupportedEncodingException {
        PrinterDesignContentBO printerDesignContentBO = printerTemplateService.loadTemplateDesignContent(templateId);
        if (printerDesignContentBO == null) {
            return new Result<>();
        }
        PrinterDesignContentVO printerDesignContentVO = new PrinterDesignContentVO();
        BeanUtils.copyProperties(printerDesignContentBO, printerDesignContentVO);
        return new Result<>(printerDesignContentVO);
    }
}
