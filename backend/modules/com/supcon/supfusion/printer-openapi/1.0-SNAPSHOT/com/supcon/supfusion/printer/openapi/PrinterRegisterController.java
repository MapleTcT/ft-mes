package com.supcon.supfusion.printer.openapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.printer.openapi.vo.PrinterRegisterVO;
import com.supcon.supfusion.printer.service.PrinterRegisterService;
import com.supcon.supfusion.printer.service.bo.PrinterRegisterBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务地址注册
 * @author liyiming
 * @date 2020/10/9 3:19 下午
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "服务地址注册", value = "服务地址注册", hidden = true)
public class PrinterRegisterController extends BaseController {
    @Autowired
    private PrinterRegisterService printerRegisterService;

    /**
     * 新增打印模板
     * @param printerRegisterVO
     * @return
     */
    @PostMapping("/regService")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="打印服务依赖的业务数据服务地址注册", httpMethod="POST")
    public Result<?> register(@RequestBody @Validated PrinterRegisterVO printerRegisterVO) {
        PrinterRegisterBO printerRegisterBO = new PrinterRegisterBO();
        BeanUtils.copyProperties(printerRegisterVO, printerRegisterBO);
        printerRegisterService.addPrinterRegister(printerRegisterBO);
        return new Result<>();
    }

}
