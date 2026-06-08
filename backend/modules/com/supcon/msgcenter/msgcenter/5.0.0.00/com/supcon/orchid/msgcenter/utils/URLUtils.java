package com.supcon.orchid.msgcenter.utils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class URLUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(URLUtils.class);

    public static void httpURLGETCase(String path) {
        actGetURL(path);
    }

    public static void httpURLPOSTCase(String path, String body) {
        actPostURL(path, body);
    }


    public static String actPostURL(String target_url, String body) {
        Boolean flag = false;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders requestHeaders_target = new HttpHeaders();
        ResponseEntity<Map> responseEntity = null;
        if (target_url.indexOf("/sendWebSocket") > -1) {
            //取消对websocket的日志拦截
            flag = true;
        }
        if (!flag) {
            LOGGER.debug("msgctr SDK trriger：" + target_url + "------body：" + body);
        }
        requestHeaders_target.add("Content-Type", "application/json;charset=utf-8");

        HttpEntity<String> requestEntity = new HttpEntity<String>(body, requestHeaders_target);
        try {
            responseEntity = restTemplate.postForEntity(target_url, requestEntity, Map.class);
        } catch (Exception e) {
            LOGGER.error("接口调用失败，请求方式：POST 接口地址：{} 请求内容：{} 请求头：{}", target_url, body, new Gson().toJson(requestHeaders_target));
            LOGGER.error(e.getMessage(), e);
        }
        if (!flag) {
            LOGGER.debug("port result：" + new Gson().toJson(responseEntity));
        }
        if (responseEntity.getStatusCodeValue() == 200) {
            return new Gson().toJson(responseEntity.getBody());
        }
        return null;
    }

    public static String actGetURL(String target_url) {
        Boolean flag = false;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders requestHeaders_target = new HttpHeaders();
        ResponseEntity<Map> responseEntity_target = null;

        if (target_url.indexOf("/sendWebSocket") > -1) {
            //取消对websocket的日志拦截
            flag = true;
        }
        if (!flag) {
            LOGGER.debug("msgctr SDK trriger：" + target_url);
        }
        requestHeaders_target.add("Content-Type", "application/json;charset=utf-8");

        HttpEntity<String> requestEntity_target = new HttpEntity<String>(null, requestHeaders_target);
        try {
            responseEntity_target = restTemplate.exchange(target_url, HttpMethod.GET, requestEntity_target, Map.class);
        } catch (Exception e) {
            LOGGER.error("接口调用失败，请求方式：GET 接口地址：{} ", target_url);
            LOGGER.error(e.getMessage(), e);
        }
        if (!flag) {
            LOGGER.debug("port result：" + new Gson().toJson(responseEntity_target));
        }
        if (responseEntity_target.getStatusCodeValue() == 200) {
            return new Gson().toJson(responseEntity_target.getBody());
        }
        return null;
    }


}
