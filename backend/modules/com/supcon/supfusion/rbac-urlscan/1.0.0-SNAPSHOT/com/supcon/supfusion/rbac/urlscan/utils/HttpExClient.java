package com.supcon.supfusion.rbac.urlscan.utils;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.supports.Charsets;
import com.supcon.supfusion.framework.cloud.common.util.JsonUtil;
import com.supcon.supfusion.framework.cloud.common.util.StringExUtil;
import com.supcon.supfusion.framework.cloud.i18n.constants.MessageRequestConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;

import java.io.*;
import java.util.Map;

/**
 * i18n服务连接处理
 *
 * @author ricky
 * @version 1.0.0
 * @date 2020-06-23 16:30
 * @copyright
 */
@Slf4j
public class HttpExClient implements Serializable {

    private static final long serialVersionUID = -5729559848816950773L;

    private static volatile HttpClient httpClient;

    /**
     * config request
     *
     * @param connectionRequestTimeout
     * @param connectTimeout
     * @return
     */
    public static RequestConfig config(int connectionRequestTimeout, int connectTimeout) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setConnectTimeout(connectTimeout)
                .build();
        return requestConfig;
    }

    /**
     * GET 方式请求
     * 参数通过 url 拼接
     *
     * @param host      请求地址
     * @param path      接口路径
     * @param paramsMap 请求参数
     * @return
     * @throws IOException
     */
    public static HttpResponse doGet(String host, String path, Map<String, String> paramsMap) {
        initHttpClient();
        HttpGet httpGet = new HttpGet(getRequestUrl(host, path, paramsMap));
        httpGet.setConfig(config(MessageRequestConstant.REQUEST_TIMEOUT_MILLIS, MessageRequestConstant.CONNECT_TIMEOUT_MILLIS));
        httpGet.setHeader(HTTP.CONTENT_TYPE, ContentType.create(ContentType.APPLICATION_FORM_URLENCODED
                .getMimeType(), Charsets.UTF_8_NAME).toString());
        try {
            return httpClient.execute(httpGet);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * POST 方式请求
     * 参数通过 url 拼接
     *
     * @param host      请求地址
     * @param path      接口路径
     * @param paramsMap 请求参数
     * @return
     * @throws IOException
     */
    public static HttpResponse doPost(String host, String path, Map<String, String> paramsMap) {
        initHttpClient();
        HttpPost httpPost = new HttpPost(getRequestUrl(host, path, paramsMap));
        httpPost.setConfig(config(MessageRequestConstant.REQUEST_TIMEOUT_MILLIS, MessageRequestConstant.CONNECT_TIMEOUT_MILLIS));
        httpPost.setHeader(HTTP.CONTENT_TYPE, ContentType.create(ContentType.APPLICATION_FORM_URLENCODED
                .getMimeType(), Charsets.UTF_8_NAME).toString());
        try {
            return httpClient.execute(httpPost);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * POST 方式请求
     * 参数通过 Body 传送,JSON 格式
     *
     * @param host       请求地址
     * @param path       接口路径
     * @param jsonParams 请求参数(json 字符串)
     * @return
     */
    public static HttpResponse doPost(String host, String path, String jsonParams) {
        initHttpClient();
        HttpPost httpPost = new HttpPost(host + path);
        StringEntity stringentity = new StringEntity(jsonParams, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringentity);
        httpPost.setConfig(config(MessageRequestConstant.REQUEST_TIMEOUT_MILLIS, MessageRequestConstant.CONNECT_TIMEOUT_MILLIS));
        httpPost.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        try {
            return httpClient.execute(httpPost);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * PUT 方式请求
     * 参数通过 Body 传送,JSON 格式
     *
     * @param host       请求地址
     * @param path       接口路径
     * @param jsonParams 请求参数(json 字符串)
     * @return
     */
    public static HttpResponse doPut(String host, String path, String jsonParams) {
        initHttpClient();
        HttpPut httpPut = new HttpPut(host + path);
        StringEntity stringentity = new StringEntity(jsonParams, ContentType.APPLICATION_JSON);
        httpPut.setEntity(stringentity);
        httpPut.setConfig(config(MessageRequestConstant.REQUEST_TIMEOUT_MILLIS, MessageRequestConstant.CONNECT_TIMEOUT_MILLIS));
        httpPut.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        try {
            return httpClient.execute(httpPut);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * POST 方式请求
     * 文件上传
     *
     * @param host             请求地址
     * @param path             接口路径
     * @param paramsMap        请求参数
     * @param fileOriginalPath 文件目录
     * @param fileOriginalName 原始文件名
     * @param name             文件对应字段名
     * @return
     */
    public static HttpResponse doPost(String host, String path, Map<String, String> paramsMap,
                                      String fileOriginalPath, String fileOriginalName, String name) {
        initHttpClient();
        HttpPost httpPost = new HttpPost(host + path);
        File file = new File(fileOriginalPath + File.separator + fileOriginalName);
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            // 解决中文文件名乱码问题
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            entityBuilder.setCharset(Charsets.UTF_8);
            ContentType contentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), Consts.UTF_8);
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                entityBuilder.addTextBody(entry.getKey(), entry.getValue(), contentType);
            }
            if (name != null && fileOriginalName != null) {
                entityBuilder.addBinaryBody(name, fileInputStream, ContentType.DEFAULT_BINARY, fileOriginalName);
            }
            httpPost.setEntity(entityBuilder.build());
            httpPost.setConfig(config(MessageRequestConstant.REQUEST_TIMEOUT_MILLIS, MessageRequestConstant.CONNECT_TIMEOUT_MILLIS));
            return httpClient.execute(httpPost);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }finally {
            try {
                assert fileInputStream != null;
                fileInputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * 初始化 httpClient
     *
     * @return
     */
    private static HttpClient initHttpClient() {
        if (httpClient == null) {
            synchronized (HttpExClient.class) {
                if (httpClient == null) {
                    httpClient = HttpClients.createDefault();
                }
            }
        }
        return httpClient;
    }

    /**
     * 获取完整请求地址(包含参数)
     * 参数拼接在 url 中
     *
     * @param host      请求地址
     * @param path      接口路径
     * @param paramsMap 请求参数
     * @return
     */
    private static String getRequestUrl(String host, String path, Map<String, String> paramsMap) {
        StringBuilder reqUrl = new StringBuilder(host).append(path);
        if (paramsMap != null && !paramsMap.isEmpty()) {
            StringBuilder params = new StringBuilder();
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                params.append(HttpConstants.URL_PARAM_SC
                        + entry.getKey()
                        + HttpConstants.URL_PARAM_EQ
                        + entry.getValue());
            }
            String paramConnector = HttpConstants.URL_PARAM_FS;
            if (!host.contains(paramConnector) && !path.contains(paramConnector)) {
                reqUrl.append(paramConnector);
                reqUrl.append(params.toString().substring(1));
            } else {
                reqUrl.append(params.toString());
            }
        }

        return reqUrl.toString();
    }

    /**
     * 解析返回对象
     *
     * @param httpResponse
     * @return
     */
    public static String response(HttpResponse httpResponse) {
        if (null != httpResponse) {
            int status = httpResponse.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == status) {
                HttpEntity httpEntity = httpResponse.getEntity();
                if (null != httpEntity) {
                    try {
                        InputStream is = httpEntity.getContent();
                        StringBuffer sb = new StringBuffer();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        return sb.toString();
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 得到result对象
     *
     * @param httpResponse
     * @return
     */
    public static Result res2Result(HttpResponse httpResponse) {
        Result result = null;
        String s = HttpExClient.response(httpResponse);
        if (StringExUtil.isNotBlank(s)) {
            result = JsonUtil.parse(s, Result.class);
        }
        return result;
    }

}
