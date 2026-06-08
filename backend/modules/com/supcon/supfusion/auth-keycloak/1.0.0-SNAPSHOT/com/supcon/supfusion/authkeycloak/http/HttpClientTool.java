package com.supcon.supfusion.authkeycloak.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JBossLog
public class HttpClientTool {
    public static final String CHARSET = "UTF-8";
    /**
     * 发送Get请求
     *
     * @param url
     * @param header
     * @param params
     * @return
     */
    public static ResponseEntity doGet(String url, Map<String, String> header,Map<String, String> params) {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(60000).setSocketTimeout(15000).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        // 由客户端执行(发送)Delete请求
        try {
            if (params != null && !params.isEmpty()) {
                List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String value = entry.getValue();
                    if (value != null) {
                        pairs.add(new BasicNameValuePair(entry.getKey(), value));
                    }
                }
                // 将请求参数和url进行拼接
                url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
            }
            // 创建Delete请求
            HttpGet httpGet = new HttpGet(url);
            if (null != header) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            // 响应模型
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, CHARSET);
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("user ====" + result);
            response.close();
            return objectMapper.readValue(result, ResponseEntity.class);
        } catch (Exception e) {
            log.info("error is ",e);
        } finally {
            closeHttpClient(httpClient);
        }
        return null;
    }

    /**
     * 发送post请求
     *
     * @param url
     * @param header
     * @param params
     * @return
     */
    public static Boolean doPost(String url, Map<String, String> header,Map<String, String> params) {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(60000).setSocketTimeout(15000).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        // 由客户端执行(发送)Delete请求
        try {
            List<NameValuePair> pairs = null;
            if (params != null && !params.isEmpty()) {
                pairs = new ArrayList<NameValuePair>(params.size());
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String value = entry.getValue();
                    if (value != null) {
                        pairs.add(new BasicNameValuePair(entry.getKey(), value));
                    }
                }
            }
            // 创建post请求
            HttpPost httpPost = new HttpPost(url);
            if (pairs != null && pairs.size() > 0) {
                httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
            }
            if (null != header) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            // 响应模型
            CloseableHttpResponse response = httpClient.execute(httpPost);
            return response.getStatusLine().getStatusCode()==200;
        } catch (Exception e) {
            log.info("error is ",e);
        } finally {
            closeHttpClient(httpClient);
        }
        return false;
    }

    /**
     * 关闭HttpClient
     *
     * @param httpClient
     */
    private static void closeHttpClient(CloseableHttpClient httpClient) {
        try {
            // 释放资源
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
