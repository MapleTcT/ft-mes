package com.supcon.supfusion.theme.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.theme.dao.po.SystemThemePO;
import com.supcon.supfusion.theme.service.SystemThemeService;
import com.supcon.supfusion.theme.webapi.vo.SystemThemeVO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "theme" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class SystemThemeInterApiController extends BaseController {

    @Autowired
    SystemThemeService systemThemeService;

    @GetMapping(value = "/systemThemes")
    @ResponseBody
    ListResult<SystemThemeVO> querySystemThemeList() {
        List<SystemThemePO> systemThemePOList = systemThemeService.querySystemThemeList();
        List<SystemThemeVO> systemThemeVOList = JSONArray.parseArray(JSON.toJSONString(systemThemePOList), SystemThemeVO.class);
        return new ListResult<>(systemThemeVOList);
    }

    @PutMapping(value = "/systemTheme")
    @ResponseBody
    void updateSystemTheme(@Validated @RequestBody SystemThemeVO systemThemeVO) {
        SystemThemePO systemThemePO = new SystemThemePO();
        BeanUtils.copyProperties(systemThemeVO, systemThemePO);
        systemThemeService.updateSystemTheme(systemThemePO);
    }

    @PostMapping("/upload")
    @ResponseBody
    public void upload(@RequestParam MultipartFile file, @RequestParam("theme") String theme){
        systemThemeService.uploadLogo(file, theme);
    }
}
