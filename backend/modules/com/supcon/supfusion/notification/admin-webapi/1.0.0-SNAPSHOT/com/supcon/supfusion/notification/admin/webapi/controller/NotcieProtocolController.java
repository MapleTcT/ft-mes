package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolTmpl;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolTmplService;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolPageVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolTmplVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ProtocolTemplateBatchDeleteRequestVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ProtocolTemplateBatchDeleteResponseVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ProtocolTemplateCountResponseVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ProtocolTemplateRequestVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ProtocolTemplateResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@ResponseBody
@Api(description = "NoticeProtocol-API", tags = {"通知方式API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class NotcieProtocolController {
    @Resource(name = "adminNoticeProtocolServiceImpl")
    private NoticeProtocolService protocolService;
    @Resource(name = "adminNoticeProtocolTmplServiceImpl")
    private NoticeProtocolTmplService protocolTmplService;
    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;


    @ApiOperation(value = "查询通知方式对应的基础模板")
    @GetMapping(value = "/notice/protocol/basetempalte")
    public ListResult<NoticeProtocolTmplVO> list(@ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String protocolId) throws Exception {
        List<NoticeProtocolTmpl> tmpls = protocolTmplService.protocolTmpl(protocolId == null ? null : Long.valueOf(protocolId));
        List<NoticeProtocolTmplVO> result = new ArrayList();
        Locale locale = LocaleContextHolder.getLocale();
        for (NoticeProtocolTmpl tmpl : tmpls) {
            if (StringUtils.hasText(tmpl.getI18nKey())) {
                String realName = messageResourceWrapper.getMessageNotBlank(tmpl.getI18nKey());
                tmpl.setName(realName);
            }
            NoticeProtocolTmplVO noticeProtocolTmplVO = BeanCopyUtil.copyBeanProperties(tmpl, NoticeProtocolTmplVO::new);
            result.add(noticeProtocolTmplVO);
        }
        return new ListResult(result);
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
    @ApiOperation(value = "分页查通知方式")
    @GetMapping(value = "/notice/protocol/protocols")
    public PageResult<NoticeProtocolPageVO> list(@ApiParam(value = "通知方式编码", required = false) @RequestParam(required = false) String code,
                                                 @ApiParam(value = "通知方式名称", required = false) @RequestParam(required = false) String name,
                                                 @ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String id,
                                                 @ApiParam(value = "是否需要包括逻辑删除的数据", required = false) @RequestParam(required = false) Boolean all,
                                                 @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                                 @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {
        if (all == null) {
            all = false;
        }
        PageResult<NoticeProtocol> pageResult;
        if (pageNo != null && pageSize != null && pageNo > -1 && pageSize > -1) {
            Page page = new Page<>(pageNo, pageSize);
            Page<NoticeProtocol> entityPage = protocolService.getPageList(code, name, id == null ? null : Long.valueOf(id), page, dataId.getDataId(), dbStringUtil, all);
            pageResult = new PageResult(entityPage.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
        } else {
            pageResult = new PageResult<>();
            List<NoticeProtocol> protoResult = protocolService.getList(code, name, id == null ? null : Long.valueOf(id), dataId.getDataId(), dbStringUtil, all);
            pageResult.setList(protoResult);
        }

        List<NoticeProtocolPageVO> result = new ArrayList<NoticeProtocolPageVO>();
        PageResult<NoticeProtocolPageVO> response;
        if (pageResult.getPagination() != null) {
            response = new PageResult<NoticeProtocolPageVO>(result, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent());
        } else {
            response = new PageResult<NoticeProtocolPageVO>();
            response.setList(result);
        }

        Locale locale = LocaleContextHolder.getLocale();
        for (NoticeProtocol protol : pageResult.getList()) {
            if (StringUtils.hasText(protol.getI18nKey())) {
                String realName = messageResourceWrapper.getMessageNotBlank(protol.getI18nKey());
                protol.setName(realName);
            }

            NoticeProtocolPageVO noticeProtocolPageVO = BeanCopyUtil.copyBeanProperties(protol, NoticeProtocolPageVO::new);
            result.add(noticeProtocolPageVO);
        }
        return response;
    }


    @ApiOperation(value = "添加默认模板")
    @PostMapping(value = "/notice/protocol/template")
    public Result<ProtocolTemplateResponseVO> addDefaultTemplate(@RequestBody @Valid @NotNull ProtocolTemplateRequestVO protocolTemplateRequestVO) {
        Long protocolId = Long.valueOf(protocolTemplateRequestVO.getProtocolId());
        Integer count = protocolService.count(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), protocolId));
        if (count == null || count == 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_PROTOCOL_DONT_EXIST);
        }

        ProtocolTemplateResponseVO protocolTemplateResponseVO = new ProtocolTemplateResponseVO();
        protocolTemplateResponseVO.setId(protocolTmplService.addProtocolTemplate(protocolTemplateRequestVO.getName(), protocolTemplateRequestVO.getTemplate(), protocolId).toString());
        protocolTemplateResponseVO.setName(protocolTemplateRequestVO.getName());
        protocolTemplateResponseVO.setTemplate(protocolTemplateRequestVO.getTemplate());
        return new Result<>(protocolTemplateResponseVO);
    }

    @ApiOperation(value = "统计当前协议下基础模板个数")
    @GetMapping(value = "/notice/protocol/template/count")
    public Result<ProtocolTemplateCountResponseVO> count(@RequestParam("protocolId") @ApiParam(value = "协议ID", required = true) @NotNull(message = "协议Id不能为空") @Pattern(regexp = "\\d+", message = "id只能为数字") String protocolId) {
        Integer count = protocolTmplService.count(Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getNoticeProtocolIdFieldName(), Long.valueOf(protocolId)));

        ProtocolTemplateCountResponseVO protocolTemplateCountResponseVO = new ProtocolTemplateCountResponseVO();
        protocolTemplateCountResponseVO.setCount(count);
        return new Result<>(protocolTemplateCountResponseVO);
    }

    @ApiOperation(value = "修改默认模板")
    @PutMapping(value = "/notice/protocol/template/{templateId}")
    public Result<ProtocolTemplateResponseVO> updateDefaultTemplate(@PathVariable("templateId") @ApiParam(value = "模板ID", required = true) @NotNull Long templateId, @RequestBody @Valid @NotNull ProtocolTemplateRequestVO protocolTemplateRequestVO) {
        Integer count = protocolTmplService.count(Wrappers.<NoticeProtocolTmpl>query().eq(NoticeProtocolTmpl.getIdFieldName(), templateId));
        if (count == null || count == 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_PROTOCOL_TEMPLATE_DONT_EXIST);
        }
        protocolTmplService.updateProtocolTemplate(protocolTemplateRequestVO.getName(), protocolTemplateRequestVO.getTemplate(), templateId);
        ProtocolTemplateResponseVO protocolTemplateResponseVO = new ProtocolTemplateResponseVO();
        protocolTemplateResponseVO.setId(templateId.toString());
        protocolTemplateResponseVO.setName(protocolTemplateRequestVO.getName());
        protocolTemplateResponseVO.setTemplate(protocolTemplateRequestVO.getTemplate());
        return new Result<>(protocolTemplateResponseVO);
    }

    @ApiOperation(value = "批量删除默认模板")
    @PostMapping(value = "/notice/protocol/template/batch")
    public Result<ProtocolTemplateBatchDeleteResponseVO> deleteDefaultTemplate(@RequestBody @Valid @NotNull ProtocolTemplateBatchDeleteRequestVO protocolTemplateBatchDeleteRequestVO) {
        List<Long> ids = new ArrayList<>();
        protocolTemplateBatchDeleteRequestVO.getIds().forEach(id -> {
            try {
                ids.add(Long.valueOf(id));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_ID_CAN_ONLY_BE_NUMBER);
            }
        });
        protocolTmplService.deleteProtocolTemplate(ids);
        return new Result<>();
    }
}

