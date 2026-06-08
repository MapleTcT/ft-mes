package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.common.utils.BeanCopyUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocolConfig;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolConfigService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeDingtalkConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeEmailConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeStationLetterConfig;
import com.supcon.supfusion.notification.admin.service.bo.NoticeSuplinkConfig;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeProtocolConfigVO;
import com.supcon.supfusion.notification.email.bootstrap.INoticeEmailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/9 20:36
 */
@ResponseBody
@Api(description = "NoticeProtoclConfig-API", tags = {"协议配置项API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
public class NoticeProtocolConfigController {
    @Resource(name = "adminNoticeProtocolConfigServiceImpl")
    private NoticeProtocolConfigService protocolConfigService;
    @Autowired
    private INoticeEmailService emailEngine;

/*    @PostMapping(value = "/notice/topic/add")
    public Result<NoticeProtocolConfig> add(@ApiParam(value="协议配置",required=true)
                                   @RequestBody NoticeProtocolConfig protocolConfig) throws Exception {
        protocolConfigService.save(protocolConfig);
        return new Result(protocolConfig);
    }*/

    /***
     * 修改邮件配置
     * @param emailConfig
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "邮件协议配置")
    @PostMapping(value = "/notice/protocolconfig/email")
    public Result<NoticeEmailConfig> update(@ApiParam(value = "邮件协议配置", required = true)
                                            @RequestBody NoticeEmailConfig emailConfig) throws Exception {
        protocolConfigService.emailConfig(emailConfig);
        return new Result(emailConfig);
    }


    /***
     * 修改站内信
     * @param stationLetterConfig
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "站内信协议配置")
    @PostMapping(value = "/notice/protocolconfig/stationletter")
    public Result<NoticeStationLetterConfig> update(@ApiParam(value = "站内信协议配置", required = true)
                                                    @RequestBody NoticeStationLetterConfig stationLetterConfig) throws Exception {
        protocolConfigService.stationLetterConfig(stationLetterConfig);
        return new Result(stationLetterConfig);
    }


    /***
     * 修改钉钉
     * @param dingtalkConfig
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "钉钉协议配置")
    @PostMapping(value = "/notice/protocolconfig/dingtalk")
    public Result<NoticeEmailConfig> updateDingtalk(@ApiParam(value = "钉钉协议配置", required = true)
                                                    @RequestBody NoticeDingtalkConfig dingtalkConfig) throws Exception {
        protocolConfigService.dingTalkConfig(dingtalkConfig);
        return new Result(dingtalkConfig);
    }

    /***
     * 修改suplink
     * @param suplinkConfig
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "suplink协议配置")
    @PostMapping(value = "/notice/protocolconfig/suplink")
    public Result<NoticeSuplinkConfig> updatesuplink(@ApiParam(value = "suplink协议配置", required = true)
                                                     @RequestBody NoticeSuplinkConfig suplinkConfig) throws Exception {
        protocolConfigService.suplinkConfig(suplinkConfig);
        return new Result(suplinkConfig);
    }

    @ApiOperation(value = "根据协议ID获取配置")
    @GetMapping("/notice/protocolconfig/protocolconfig")
    public Result<NoticeProtocolConfigVO> protocolconfig(@ApiParam(value = "站内信协议配置", required = true) @RequestParam String protocolId) {
        QueryWrapper<NoticeProtocolConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeProtocolConfig.getProtocolFieldName(), protocolId);
        NoticeProtocolConfig protocolConfig = protocolConfigService.getOne(queryWrapper);
        if (protocolConfig == null) {
            return new Result();
        }

        NoticeProtocolConfigVO noticeProtocolConfigVO = BeanCopyUtil.copyBeanProperties(protocolConfig, NoticeProtocolConfigVO::new);
        return new Result<>(noticeProtocolConfigVO);
    }

    @ApiOperation(value = "验证邮箱配置是否有效", notes = "邮件仅发给发送者邮箱")
    @GetMapping("/notice/valid/emailconfig")
    public Result<Boolean> mailValid(@ApiParam(value = "接收邮箱地址", required = true) @RequestParam String username,
                                     @ApiParam(value = "发送者邮箱密码", required = true) @RequestParam String password,
                                     @ApiParam(value = "邮件服务器地址") @RequestParam(required = false) String host,
                                     @ApiParam(value = "邮件服务器端口") @RequestParam(required = false) String port,
                                     @ApiParam(value = "是否启用smtp的ssl协议") @RequestParam(required = false) Boolean enableSSL,
                                     @ApiParam(value = "邮件协议类型") @RequestParam(required = false) String emailProtocol) {
        System.out.println("--------------" + emailEngine);
        Boolean result = emailEngine.validEmailconfig(username, password, host, port, enableSSL, emailProtocol);
        return new Result<>(result);
    }


    @ApiOperation(value = "新增协议配置")
    @PostMapping("/notice/protocolconfig/add")
    public Result<NoticeProtocolConfigVO> protocolconfigSave(@ApiParam(value = "新增协议配置", required = true)
                                                             @RequestBody NoticeProtocolConfig protocolConfig) {
        protocolConfig = protocolConfigService.addProtocolConfig(protocolConfig);
        if (protocolConfig == null) {
            return new Result<>();
        }
        NoticeProtocolConfigVO noticeProtocolConfigVO = BeanCopyUtil.copyBeanProperties(protocolConfig, NoticeProtocolConfigVO::new);
        return new Result<>(noticeProtocolConfigVO);
    }


    @ApiOperation(value = "获取配置")
    @GetMapping("/notice/protocolconfig")
    public ListResult<NoticeProtocolConfigVO> protocolconfig() {
        List<NoticeProtocolConfig> protocolConfigs = protocolConfigService.list();
        if (protocolConfigs == null || protocolConfigs.size() == 0) {
            return new ListResult();
        }
        List<NoticeProtocolConfigVO> protocolConfigVOS = BeanCopyUtil.copyListProperties(protocolConfigs, NoticeProtocolConfigVO::new);
        return new ListResult(protocolConfigVOS);
    }

    @ApiOperation(value = "修改配置")
    @PutMapping("/notice/protocolconfig/{protocolId}")
    public Result updateConfig(@ApiParam(value = "协议ID", required = true) @PathVariable("protocolId") String protocolId, @ApiParam(value = "协议配置", required = true) @RequestBody NoticeProtocolConfig protocolConfig) {
        NoticeProtocolConfig noticeProtocolConfig = new NoticeProtocolConfig();
        noticeProtocolConfig.setId(Long.valueOf(protocolId));
        noticeProtocolConfig.setConfigValue(protocolConfig.getConfigValue());
        protocolConfigService.updateById(noticeProtocolConfig);
        return new Result();
    }

    @ApiOperation(value = "删除配置")
    @DeleteMapping("/notice/protocolconfig/{protocolId}")
    public Result deleteConfig(@ApiParam(value = "协议ID", required = true) @PathVariable("protocolId") String protocolId) {
        protocolConfigService.removeById(Long.valueOf(protocolId));
        return new Result();
    }

}
