package com.supcon.supfusion.systemconfig.common.client.util;


import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.systemconfig.common.client.vo.CatalogsVo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author lifangyuan
 */
@Slf4j
public class HttpClient {

    private static int TIME_OUT = 5;

    private static OkHttpClient okHttpClient;

    private HttpClient() {
    }

    static {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.connectTimeout(TIME_OUT, TimeUnit.SECONDS);
        okHttpBuilder.readTimeout(TIME_OUT, TimeUnit.SECONDS);
        okHttpBuilder.writeTimeout(TIME_OUT, TimeUnit.SECONDS);
        okHttpClient = okHttpBuilder.build();
    }

    public static boolean publishConfig(String serverAddr, CatalogsVo catalogs) throws IOException {
        String url = "http" + "://" + serverAddr + "/open-api/systemconfig/v1/config/catalog";
        MediaType mediaType = MediaType.parse("application/json");
        final Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, JSON.toJSONString(catalogs)))
                .build();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (response.isSuccessful()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean deleteConfig(String serverAddr, String appCode, String code) throws IOException {
        String url = "http" + "://" + serverAddr + "/open-api/systemconfig/v1/config/catalog" + "/" + appCode + "/" + code;
        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .delete()
                .build();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (response.isSuccessful()) {
            return true;
        } else {
            return false;
        }
    }
}
