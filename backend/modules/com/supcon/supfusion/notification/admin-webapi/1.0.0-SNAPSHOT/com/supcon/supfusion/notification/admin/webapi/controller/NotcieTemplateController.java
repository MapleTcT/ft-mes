package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolTmplService;
import com.supcon.supfusion.notification.admin.service.NoticeTemplateService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTemplateBO;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeTemplateWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolTmplVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTemplateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@ResponseBody
@Api(tags = {"消息模板API"}, description = "NoticeTemplate-API")
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
public class NotcieTemplateController {
    @Resource(name = "adminNoticeTemplateServiceImpl")
    private NoticeTemplateService templateService;
    @Autowired
    private NoticeTemplateWapper templateWapper;
    @Resource(name = "adminNoticeProtocolTmplServiceImpl")
    private NoticeProtocolTmplService protocolTmplService;
    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;


    /**
     * 不分页查询对象
     *
     * @param topicId
     * @param noticeTypeId
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "查询消息模板列表")
    @GetMapping(value = "/notice/template/topictmplmap")
    public ListResult<Map<String, Object>> topictmpllist(@ApiParam(value = "消息主题ID", required = true) @RequestParam String topicId,
                                                         @ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String noticeTypeId
    ) throws Exception {
        Locale locale = LocaleContextHolder.getLocale();
        Map<NoticeProtocol, NoticeTemplate> entityMap = templateService.queryTopicTmplRel(topicId == null ? null : Long.valueOf(topicId), noticeTypeId == null ? null : Long.valueOf(noticeTypeId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<NoticeProtocol, NoticeTemplate> entry : entityMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("protocol_id", entry.getKey().getId() != null ? entry.getKey().getId().toString() : null);
            map.put("protocol_code", entry.getKey().getProtocol());
            if (StringUtils.hasText(entry.getKey().getI18nKey())) {
                map.put("protocol_name", messageResourceWrapper.getMessageNotBlank(entry.getKey().getI18nKey()));
            } else {
                map.put("protocol_name", entry.getKey().getName());
            }
            map.put("id", entry.getValue().getId() != null ? entry.getValue().getId().toString() : null);
            map.put("name", entry.getValue().getName());
            map.put("code", entry.getValue().getCode());
            map.put("template", entry.getValue().getTemplate());
            map.put("params", entry.getValue().getParams());
            map.put("memo", entry.getValue().getMemo());
            result.add(map);
        }
        return new ListResult(result);
    }


    @ApiOperation(value = "根据通知方式获取默认消息模板信息")
    @GetMapping(value = "/notice/template/defulttmpl")
    public Result<NoticeProtocolTmplVO> defulttmpl(@ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String noticeTypeId) throws Exception {
        NoticeProtocolTmpl noticeProtocolTmpl = protocolTmplService.protocolDefaultTmpl(noticeTypeId == null ? null : Long.valueOf(noticeTypeId));
        if (noticeProtocolTmpl == null) {
            return new Result();
        }
        NoticeProtocolTmplVO noticeProtocolTmplVO = BeanCopyUtil.copyBeanProperties(noticeProtocolTmpl, NoticeProtocolTmplVO::new);
        return new Result(noticeProtocolTmplVO);
    }

    /**
     * 分页查询对象
     *
     * @param code
     * @param name
     * @param id
     * @param pageNo
     * @param pageSize
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "分页查询消息模板")
    @GetMapping(value = "/notice/template/templates")
    public PageResult<NoticeTemplateVO> list(@ApiParam(value = "消息模板编码", required = false) @RequestParam(required = false) String code,
                                             @ApiParam(value = "消息模板名称", required = false) @RequestParam(required = false) String name,
                                             @ApiParam(value = "消息模板ID", required = false) @RequestParam(required = false) String id,
                                             @ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String noticeTypeIds,
                                             @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                             @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {

        if (pageNo != null && pageSize != null && pageNo > -1 && pageSize > -1) {
            Page page = new Page<>(pageNo, pageSize);
            Page<NoticeTemplate> entityPage = templateService.queryPageList(code, name, id == null ? null : Long.valueOf(id), noticeTypeIds, page);
            Page<NoticeTemplateVO> wapper = templateWapper.pageCP(entityPage);
            return new PageResult<NoticeTemplateVO>(wapper.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        } else {
            PageResult<NoticeTemplateVO> pageResult = new PageResult<>();
            List<NoticeTemplate> entityList = templateService.queryList(code, name, id == null ? null : Long.valueOf(id), noticeTypeIds);
            List<NoticeTemplateVO> wapper = templateWapper.listCP(entityList);
            pageResult.setList(wapper);
            return pageResult;
        }
    }

    /**
     * 关键字查询
     *
     * @param code
     * @param name
     * @param noticeTypeIds
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/template/keyword")
    public ListResult<NoticeTemplateVO> mnemonicList(@ApiParam(value = "消息模板编码", required = false) @RequestParam(required = false) String code,
                                                     @ApiParam(value = "消息模板名称", required = false) @RequestParam(required = false) String name,
                                                     @ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String noticeTypeIds) throws Exception {
        List<NoticeTemplate> result = templateService.queryListByKeyword(code, name, noticeTypeIds);
        List<NoticeTemplateVO> wapper = templateWapper.listCP(result);
        return new ListResult(wapper);
    }

    /***
     * 新增
     * @param templateBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/template/add")
    public Result<NoticeTemplateVO> add(@ApiParam(value = "消息模板对象", required = true) @RequestBody NoticeTemplateBO templateBO) throws Exception {
        NoticeTemplate template = templateBO.entityCP(templateBO);
        NoticeTemplate result = templateService.addEntity(template);
        NoticeTemplateVO wapper = templateWapper.entityCP(result);
        return new Result(wapper);
    }

    /***
     * 修改
     * @param templateBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/template/update")
    public Result<NoticeTemplateVO> update(@ApiParam(value = "消息模板对象", required = true) @RequestBody NoticeTemplateBO templateBO) throws Exception {
        NoticeTemplate template = templateBO.entityCP(templateBO);
        template.setCoverSign(0);
        NoticeTemplate result = templateService.updateEntity(template);
        NoticeTemplateVO wapper = templateWapper.entityCP(result);
        return new Result(wapper);
    }

    /***
     * 删除
     * @param ids 多条id逗号分隔
     * @return
     * @throws Exception
     */
    @DeleteMapping("/notice/template/delete")
    public Result<String> delete(@ApiParam(value = "数据库ID", example = "1000,1003,1004", required = true) @RequestParam String ids) throws Exception {
        String result = templateService.delEntity(ids);
        return new Result(result);
    }

}
