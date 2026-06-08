package com.supcon.supfusion.printer.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.printer.api.dto.PrinterRegisterDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@FeignClient(name = "printer")
public interface PrinterApiService {

    String API_PREFIX = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "printer" + HttpConstants.URL_SPLITER + "v2";

    /**
     * 新增打印模板
     * @param printerRegisterDTO
     * @return
     */
    @PostMapping(value = API_PREFIX + "/regService")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Boolean register(@RequestBody PrinterRegisterDTO printerRegisterDTO);
}
