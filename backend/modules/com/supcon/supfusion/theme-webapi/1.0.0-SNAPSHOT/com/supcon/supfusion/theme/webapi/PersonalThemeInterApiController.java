package com.supcon.supfusion.theme.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.theme.dao.po.PersonalThemePO;
import com.supcon.supfusion.theme.service.PersonalThemeService;
import com.supcon.supfusion.theme.webapi.vo.PersonalThemeVO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Objects;

@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "theme" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class PersonalThemeInterApiController extends BaseController {

    @Autowired
    PersonalThemeService personalThemeService;

    @GetMapping(value = "/personalThemes")
    @ResponseBody
    ListResult<PersonalThemeVO> queryPersonalThemeList() {
        List<PersonalThemePO> personalThemePOList = personalThemeService.queryPersonalThemeList();
        List<PersonalThemeVO> personalThemeVOList = JSONArray.parseArray(JSON.toJSONString(personalThemePOList), PersonalThemeVO.class);
        return new ListResult<>(personalThemeVOList);
    }

    @GetMapping(value = "/personalTheme")
    @ResponseBody
    Result<PersonalThemeVO> queryPersonalTheme() {
        PersonalThemePO personalThemePO = personalThemeService.queryPersonalTheme();
        if (Objects.isNull(personalThemePO)) {
            return new Result<>();
        }
        PersonalThemeVO personalThemeVO = new PersonalThemeVO();
        BeanUtils.copyProperties(personalThemePO, personalThemeVO);
        return new Result<>(personalThemeVO);
    }

    @PutMapping(value = "/personalTheme")
    void updatePersonalTheme(@Validated @RequestBody PersonalThemeVO personalThemeVO) {
        PersonalThemePO personalThemePO = new PersonalThemePO();
        BeanUtils.copyProperties(personalThemeVO, personalThemePO);
        personalThemeService.updatePersonalTheme(personalThemePO);
    }

    @DeleteMapping(value = "/personalTheme")
    void deletePersonalTheme() {
        personalThemeService.deletePersonalTheme();
    }

}
