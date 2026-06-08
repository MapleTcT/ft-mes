package com.supcon.supfusion.notification.app.config.util;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/21 14:30
 */
public class UrlUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(UrlUtils.class);

    /***
     * Post方式
     * @param target_url 目标地址
     * @param headerMap  header信息
     * @param body body信息
     * @return
     */
    public static String actPostURL(String target_url, Map<String, String> headerMap, String body) {
        LOGGER.info("发送到指定推送渠道地址：{}, 内容：{}, 请求头：{}", target_url, body, JSON.toJSONString(headerMap));
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> responseEntity = null;
        HttpHeaders requestHeaders_target = new HttpHeaders();
        requestHeaders_target.add("Content-Type", "application/json;charset=utf-8");
        if (null != headerMap && headerMap.size() > 0) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                requestHeaders_target.add(entry.getKey(), entry.getValue());
            }
        }
        HttpEntity<String> requestEntity = new HttpEntity<String>(body, requestHeaders_target);
        try {
            responseEntity = restTemplate.postForEntity(target_url, requestEntity, Map.class);
        } catch (Exception e) {
            LOGGER.error("接口调用失败，请求方式：POST 接口地址：{} 请求内容：{} 请求头：{}", target_url, body, JSON.toJSONString(headerMap));
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("返回值：{}", JSON.toJSONString(responseEntity));
        if (responseEntity != null && responseEntity.getStatusCodeValue() == 200) {
            return JSON.toJSONString(responseEntity.getBody());
        }
        return null;
    }

    public static String actGetURL(String target_url, Map<String, String> headerMap) {
        LOGGER.info("发送到指定推送渠道地址：{}, 请求头：{}", target_url, JSON.toJSONString(headerMap));
        String response = actGetURLWithoutLog(target_url, headerMap);
        LOGGER.info("返回值：{}", JSON.toJSONString(response));
        return response;
    }

    public static String actGetURLWithoutLog(String target_url, Map<String, String> headerMap) {
        ResponseEntity<Map> responseEntity_target = null;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders requestHeaders_target = new HttpHeaders();
        requestHeaders_target.add("Content-Type", "application/json;charset=utf-8");
        if (null != headerMap && headerMap.size() > 0) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                requestHeaders_target.add(entry.getKey(), entry.getValue());
            }
        }
        HttpEntity<String> requestEntity_target = new HttpEntity<String>(null, requestHeaders_target);
        try {
            responseEntity_target = restTemplate.exchange(target_url, HttpMethod.GET, requestEntity_target, Map.class);
        } catch (Exception e) {
            LOGGER.error("接口调用失败，请求方式：GET 接口地址：{} ", target_url);
            LOGGER.error(e.getMessage(), e);
        }
        if (responseEntity_target != null && responseEntity_target.getStatusCodeValue() == 200) {
            return JSON.toJSONString(responseEntity_target.getBody());
        }
        return null;
    }


}
