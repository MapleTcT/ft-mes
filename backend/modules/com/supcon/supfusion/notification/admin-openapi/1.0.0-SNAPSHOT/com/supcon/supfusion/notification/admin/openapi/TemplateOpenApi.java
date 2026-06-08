package com.supcon.supfusion.notification.admin.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.openapi.vo.NoticeTemplateVO;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 新增模板
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = {"新增模板接口", "open-api"})
public class TemplateOpenApi extends BaseController {


    @Resource(name = "adminNoticeTemplateServiceImpl")
    private NoticeTemplateService templateService;


    /**
     * 新增模板
     *
     * @param noticeTemplate
     * @return
     */
    @PostMapping(value = "/template")
    @ResponseBody
    @ApiOperation("新增模板")
    public Result<Map> register(@RequestBody @Valid @ApiParam(name = "协议配置参数", value = "传入json格式", required = true) NoticeTemplateVO noticeTemplate) {
        NoticeTemplate template = noticeTemplate.entityCP(noticeTemplate);
        templateService.addEntity(template);
        Map map = new HashMap();
        map.put("tempId", template.getId().toString());
        return new Result<>(map);
    }

}
