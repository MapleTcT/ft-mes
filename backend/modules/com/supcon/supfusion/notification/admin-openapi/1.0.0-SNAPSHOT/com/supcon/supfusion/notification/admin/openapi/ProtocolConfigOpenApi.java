package com.supcon.supfusion.notification.admin.openapi;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.notification.admin.openapi.vo.ProtocolConfigListVO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 获取协议类型列表
 *
 * @param
 * @return
 */
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = {"获取协议类型列表接口", "open-api"})
public class ProtocolConfigOpenApi extends BaseController {

    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;

    /**
     * 获取协议类型列表
     *
     * @return
     */
    @GetMapping(value = "/protocols")
    @ResponseBody
    @ApiOperation("获取协议类型列表")
    public Result<ProtocolConfigListVO> protocolList() {
        ProtocolConfigListVO protocolConfig = new ProtocolConfigListVO();
        List<NoticeProtocol> list = noticeProtocolMapper.selectList(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
        protocolConfig.setProtocols(protocolConfig.entityCP(list));
        return new Result<>(protocolConfig);
    }

}
