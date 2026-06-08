package com.supcon.supfusion.auth.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpClientUtils {

    // 建立连接的事件
    private static final int HTTP_CONNECTION_TIMEOUT = 5000;
    // 数据等待时间（两个连续数据包之间的最大数据间隔）
    private static final int HTTP_SOCKET_TIMEOUT = 60000;
    // 从 connect Manager 获取连接的最大时长
    // 使用 HttpClientBuilder.create().build() 获取 httpClient 时，会内置一个 HttpClientConnectionManager
    private static final int HTTP_CONNECTION_MANAGER_TIMEOUT = 5000;

    private static final RequestConfig config = RequestConfig.custom().
            setConnectTimeout(HTTP_CONNECTION_TIMEOUT).
            setConnectionRequestTimeout(HTTP_CONNECTION_MANAGER_TIMEOUT).
            setSocketTimeout(HTTP_SOCKET_TIMEOUT).build();

    /**
     * 封装HTTP POST方法
     *
     * @param
     * @param （如JSON串）
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String post(String url, Map<String, String> header, String data) throws ClientProtocolException, IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(url);
            if (null != header) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            if (StringUtils.isNoneEmpty(data)) {
                StringEntity se = new StringEntity(data, "UTF-8");
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se);
            }
            String httpEntityContent;
            try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
                httpEntityContent = getHttpEntityContent(response);
            }
            return httpEntityContent;
        }
    }

    /**
     * 封装HTTP GET方法
     *
     * @param
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String get(String url) throws ClientProtocolException, IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet();
            httpGet.setURI(URI.create(url));
            // 获取对象， 绑定信息等查询时设置超时
            httpGet.setConfig(config);
            String httpEntityContent;
            try (CloseableHttpResponse response = httpClient.execute(httpGet);) {
                httpEntityContent = getHttpEntityContent(response);
            }
            log.debug("httpGet response info is : {}", httpEntityContent);

            return httpEntityContent;
        }
    }

    /**
     * 封装HTTP GET方法
     *
     * @param
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static String get(String url, Map<String, String> headers) throws ClientProtocolException, IOException {
        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet();
            httpGet.setURI(URI.create(url));
            // 获取对象， 绑定信息等查询时设置超时
            httpGet.setConfig(config);
            if (headers != null && headers.size() > 0) {
                headers.forEach((k, v) -> {
                    httpGet.setHeader(k, v);
                });
            }
            String httpEntityContent;
            try (CloseableHttpResponse response = httpClient.execute(httpGet);) {
                httpEntityContent = getHttpEntityContent(response);
            }
            return httpEntityContent;
        }
    }

    /**
     * 获得响应HTTP实体内容
     *
     * @param response
     * @return
     */
    private static String getHttpEntityContent(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (InputStream is = entity.getContent();
                 InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                 BufferedReader br = new BufferedReader(isr);) {
                String line = br.readLine();
                StringBuilder sb = new StringBuilder();
                while (line != null) {
                    sb.append(line + "\n");
                    line = br.readLine();
                }
                return sb.toString();
            } catch (Exception e) {
                log.error("GetHttpEntityContent failed", e);
            }
        }
        return "";
    }

    /**
     * 将url参数转换成map
     *
     * @param param aa=11&bb=22&cc=33
     * @return
     */
    public static Map<String, Object> getUrlParams(String param) {
        Map<String, Object> map = new HashMap<String, Object>(0);
        if (StringUtils.isBlank(param)) {
            return map;
        }
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    /**
     * 将map转换成url
     *
     * @param map
     * @return
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }


    public static String getHostPort(String url) {

        try {
            log.info("url=====> {} ");
            final URL uri = new URL(url);
            final String host = uri.getHost();
            final int port = uri.getPort();

            String format = String.format("%s://%s", uri.getProtocol(), host + (port == -1 ? "" : ":" + port));
            return format;

        } catch (Exception e2) {
            return null;
        }


    }

    public static void main(String[] args) {
        try {

            final URL uri = new URL("http://ess-10xz.stable2.test.supos.net/inter-api/auth/v1/third/authorize?protocolType=bluetron");
            final String host = uri.getHost();
            final int port = uri.getPort();

            String format = String.format("%s://%s", uri.getProtocol(), host + (port == -1 ? "" : ":" + port));
            System.out.printf(format);

        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

}
