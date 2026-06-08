package com.supcon.supfusion.notification.engine.dispatcher.protocol;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.common.util.JSONUtil;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.engine.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.protocol.Protocol;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.Notice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Service
@Slf4j
public class RemoteProtocol implements Protocol {

    @Resource(name = "engineNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Ack send(Notice notice) {
        String protocol = notice.getProtocol();
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        if (noticeProtocol == null) {
            return Ack.buildFail(notice, String.format("%s protocol dont exist", notice.getProtocol()), ReadStatus.UNKOWN, SendStatus.FAIL);
        }
        String serviceName = noticeProtocol.getServiceName();
        String uri = noticeProtocol.getSendUrl();
        StringBuilder url = new StringBuilder();
        if (!serviceName.startsWith("http")) {
            url.append("http://");
            url.append(serviceName);
        }
        if (!uri.startsWith("/")) {
            url.append("/");
            url.append(uri);
        } else {
            url.append(uri);
        }
        return pushMessage(notice, url.toString());
    }

    private Ack pushMessage(Notice notice, String url) {
        ResponseEntity<String> responseEntity = null;

        HttpHeaders requestHeaders_target = new HttpHeaders();
        requestHeaders_target.add("Content-Type", "application/json;charset=utf-8");
        requestHeaders_target.add("X-Tenant-Id", RpcContext.getContext() == null ? null : RpcContext.getContext().getTenantId());

        HttpEntity<String> requestEntity = new HttpEntity(JSONUtil.toJSONString(notice), requestHeaders_target);
        try {
            log.debug("接口调用请求方式：POST 接口地址：{} 请求内容：{} 请求头：{}", url, notice.toString(), requestHeaders_target.toString());
            responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception e) {
            log.error("接口调用失败，POST 接口地址：{} 请求内容：{} 请求头：{}", url, notice.toString(), requestHeaders_target.toString());
            log.error(e.getMessage(), e);
            return Ack.buildFail(notice, "The HTTP interface call failed", ReadStatus.UNKOWN, SendStatus.FAIL);
        }
        try {
            Result<Ack> result = JSONUtil.parseToObject(responseEntity.getBody(), new TypeReference<Result<Ack>>() {
            });
            if (responseEntity.getStatusCode().value() >= 200 && responseEntity.getStatusCode().value() < 300) {
                log.debug("接口调用成功，response：{}", result.toString());
                return result.getData();
            } else {
                return Ack.buildFail(notice, result.getMessage(), ReadStatus.UNKOWN, SendStatus.FAIL);
            }
        } catch (Exception e) {
            log.error("Response解析失败，protocol:{}, status：{}, body : {}", notice.getProtocol(), responseEntity.getStatusCode(), responseEntity.getBody());
            log.error(e.getMessage(), e);
            return Ack.buildFail(notice, String.format("%s Response parse failure", notice.getProtocol()), ReadStatus.UNKOWN, SendStatus.FAIL);
        }
    }

}
