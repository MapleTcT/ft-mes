package com.supcon.supfusion.printer.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.printer.openapi.vo.EntityPageUrlVO;
import com.supcon.supfusion.printer.service.PrinterRegisterService;
import com.supcon.supfusion.printer.service.bo.EntityPageUrlBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "打印模块实体注册管理", value = "实体注册管理说明文档", hidden = true)
public class PrinterEntityRegisterController {

    @Autowired
    private PrinterRegisterService printerRegisterService;
    /**
     * 实体对象注册页面地址
     * @param entityPageUrlVO 实体注册内容
     */
    @PostMapping("/regEntityPageUrl")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="实体对象注册页面地址", httpMethod="POST")
    public void registerEntityPageUrl(@RequestBody EntityPageUrlVO entityPageUrlVO) {
        EntityPageUrlBO entityPageUrlBO = new EntityPageUrlBO();
        BeanUtils.copyProperties(entityPageUrlVO, entityPageUrlBO);
        printerRegisterService.registerEntityPageUrl(entityPageUrlBO);
    }
}
