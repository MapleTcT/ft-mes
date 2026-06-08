package com.supcon.supfusion.notification.admin.webapi.controller;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTmplRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/9 20:36
 */
@ResponseBody
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
public class NoticeTopicTmplRelController {
    @Resource(name = "adminNoticeTopicTmplRelServiceImpl")
    private NoticeTopicTmplRelService topicTmplRelService;

   /* @ApiOperation(value = "分页查询消息主题模板关联")
    @GetMapping(value = "/notice/topictmplrel/list")
    public Result<PageList<NoticeTopicTmplateRelation>> list(@ApiParam(name="消息主题模板关联编码",value="defultTopic",required=false)String code,
                                                             @ApiParam(name="消息主题模板关联名称",value="默认消息主题模板关联",required=false)String name,
                                                             @ApiParam(name="消息主题模板关联ID",value="1000",required=false)Long id,
                                                             @ApiParam(name="页码",value="0",required=false)int pageNo,
                                                             @ApiParam(name="分页大小",value="20",required=false)int pageSize) throws Exception {
        PageList<NoticeTopicTmplateRelation> entityPage = topicTmplRelService.queryPageList(code, name, id, pageNo, pageSize);
        return new Result(entityPage);
    }*/


}
