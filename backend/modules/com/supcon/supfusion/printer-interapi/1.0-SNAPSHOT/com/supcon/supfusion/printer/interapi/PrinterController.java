package com.supcon.supfusion.printer.interapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.printer.interapi.dto.RemotePrinterDTO;
import com.supcon.supfusion.printer.interapi.vo.PrinterLogVO;
import com.supcon.supfusion.printer.interapi.vo.PagePrinterTemplateResultVO;
import com.supcon.supfusion.printer.service.PrinterLogService;
import com.supcon.supfusion.printer.service.PrinterService;
import com.supcon.supfusion.printer.service.bo.PagePrinterTemplateResultBO;
import com.supcon.supfusion.printer.service.bo.PrinterLogBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印基础控制类
 * @author liyiming
 * @date 2020/10/9 3:04 下午
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "打印管理", value = "打印管理文档说明", hidden = true)
public class PrinterController extends BaseController {

    @Autowired
    private PrinterLogService printerLogService;

    @Autowired
    private PrinterService printerService;
    /**
     * 远程打印接口
     * 接收前端传递的打印信息和文件信息，调用sdk方法上传到print remote server
     * @param remotePrinterDTO
     * @return
     */
/*    @PostMapping("/remote/print")
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value="远程打印接口", httpMethod="POST")
    public Result<?> doCreate(@RequestBody @Validated RemotePrinterDTO remotePrinterDTO) {
        Object obj = null;
        return new Result<>(obj);
    }*/

    /**
     * 打印时记录打印日志
     * @param printerLogVO 日志信息
     */
    @PostMapping("/log")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "打印时添加打印日志", httpMethod = "POST")
    public void addPrinterLog(@Validated @RequestBody PrinterLogVO printerLogVO) {

        PrinterLogBO printerLogBO = new PrinterLogBO();
        BeanUtils.copyProperties(printerLogVO, printerLogBO);
        printerLogService.addPrinterLog(printerLogBO);
    }

    /**
     * 根据页面id查询打印列表
     * @param pageId
     * @return
     */
    @GetMapping("/page/{pageId}/printerTemplates")
    @ApiOperation(value = "根据页面id查询打印模板列表", httpMethod = "GET")
    public ListResult<PagePrinterTemplateResultVO> listPrinterTemplatesByPageId(@PathVariable(value = "pageId") String pageId) {

        List<PagePrinterTemplateResultBO> resultBOS = printerService.listTemplatesByPageId(pageId);
        if (resultBOS == null || resultBOS.size() == 0) {
            return new ListResult<>();
        }

        List<PagePrinterTemplateResultVO> list = new ArrayList<>();
        resultBOS.stream().forEach(item -> {
            PagePrinterTemplateResultVO pagePrinterTemplateResultVO = new PagePrinterTemplateResultVO();
            BeanUtils.copyProperties(item, pagePrinterTemplateResultVO);
            list.add(pagePrinterTemplateResultVO);
        });
        return new ListResult<PagePrinterTemplateResultVO>(list);
    }
}
