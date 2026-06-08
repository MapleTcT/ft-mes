package com.supcon.supfusion.notification.admin.openapi;

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.manager.service.IManagerTopicService;
import com.supcon.supfusion.notification.admin.openapi.vo.KeyWordResponseVO;
import com.supcon.supfusion.notification.admin.openapi.vo.KeyWordVO;
import com.supcon.supfusion.notification.admin.openapi.vo.NoticeTopicListVO;
import com.supcon.supfusion.notification.admin.openapi.vo.NoticeTopicVO;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知中心Topic管理
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = {"外部协议注册接口", "open-api"})
public class TopicOpenApi extends BaseController {
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService noticeTopicService;
    @Autowired
    private IManagerTopicService managerTopicService;

    /**
     * 获取主题所带模板的所有占位变量
     *
     * @return
     */
    @GetMapping(value = "/topic/keywords")
    @ResponseBody
    @ApiOperation("获取主题所带模板的所有占位变量")
    public Result<KeyWordResponseVO> keywords(@RequestParam("topicCode") @NotEmpty(message = "主题编号不能为空") @ApiParam(name = "topicCode", value = "协议配置参数", required = true) String topicCode) {
        List<String> keywords = noticeTopicService.keywords(topicCode);
        List<KeyWordVO> keyWordVOS = new ArrayList<>();
        for (String keyword : keywords) {
            KeyWordVO keyWordVO = new KeyWordVO();
            keyWordVO.setKey(keyword);
            keyWordVOS.add(keyWordVO);
        }
        KeyWordResponseVO keyWordResponseVO = new KeyWordResponseVO();
        keyWordResponseVO.setKeyWordVOS(keyWordVOS);
        return new Result<>(keyWordResponseVO);
    }

    /**
     * 获取主题列表
     *
     * @param
     * @return
     */
    @GetMapping(value = "/topics")
    @ResponseBody
    @ApiOperation("获取主题列表接口")
    public Result<NoticeTopicListVO> topicList() {
        NoticeTopicListVO noticeTopicList = new NoticeTopicListVO();
        List<NoticeTopic> topic = noticeTopicService.queryList(null, null, null);
        noticeTopicList.setTopics(noticeTopicList.entityCP(topic));
        return new Result<>(noticeTopicList);
    }

    /**
     * 新增主题
     *
     * @param noticeTopic
     * @return
     */
    @PostMapping(value = "/topic")
    @ResponseBody
    @ApiOperation("新增主题")
    public Result<Map> register(@RequestBody @Valid @ApiParam(name = "协议配置参数", value = "传入json格式", required = true) NoticeTopicVO noticeTopic) {
        NoticeTopic topic = noticeTopic.entityCP(noticeTopic);
        /*NoticeTopic result = topicService.addEntity(topic);*/
        managerTopicService.addTopicAndRangeType(topic);
        Map map = new HashMap();
        map.put("tempId", topic.getId().toString());
        return new Result<>(map);
    }
}
