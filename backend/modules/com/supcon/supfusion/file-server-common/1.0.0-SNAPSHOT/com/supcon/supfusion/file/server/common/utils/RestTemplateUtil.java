package com.supcon.supfusion.file.server.common.utils;

import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Properties;

@Slf4j
public class RestTemplateUtil {

    public static Boolean getAuthentication(String Authorization, RestTemplate restTemplate,
                                            String methodType, String serverName, String url,
                                            String entityCode, String linkId, String id,
                                            String type, Long mainModelId, String propertyCode,
                                            String showType, Long userId) {
        if (!url.startsWith(Constants.PATH)) {
            url = Constants.PATH + url;
        }
        String urlAll = Constants.HTTP_HEAD_PATH + serverName + url;
        String params = "";
        if (entityCode != null) {
            params = params + "entityCode=" + entityCode;
        } else {
            params = params + "entityCode=";
        }
        if (linkId != null) {
            params = params + Constants.HE + "linkId=" + linkId;
        } else {
            params = params + Constants.HE + "linkId=";
        }
        if (id != null) {
            params = params + Constants.HE + "id=" + id;
        } else {
            params = params + Constants.HE + "id=";
        }
        if (type != null) {
            params = params + Constants.HE + "type=" + type;
        } else {
            params = params + Constants.HE + "type=";
        }
        if (mainModelId != null) {
            params = params + Constants.HE + "mainModelId=" + mainModelId;
        } else {
            params = params + Constants.HE + "mainModelId=";
        }
        if (propertyCode != null) {
            params = params + Constants.HE + "propertyCode=" + propertyCode;
        } else {
            params = params + Constants.HE + "propertyCode=";
        }
        if (showType != null) {
            params = params + Constants.HE + "showType=" + showType;
        } else {
            params = params + Constants.HE + "showType=";
        }

        log.info("file-server-restTemplate:" + urlAll + Constants.QUE + params);
        Boolean isCanDownload = false;
        if (methodType.toUpperCase().equals(Constants.METHOD_GET)) {
//            Map<String, Object> resultMap = restTemplate.getForObject(urlAll + Constants.QUE + params, Map.class);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization",Authorization);
            ResponseEntity<Map> res = restTemplate.exchange(urlAll + Constants.QUE + params, HttpMethod.GET, new HttpEntity<>(null, headers),Map.class);
            log.info("restTemplate get with selfDefine header: {}", res);
            Map resultMap = res.getBody();
            //返回：
//            {
//                "code": 200,
//                "success": true,
//                "data": true,
//                "msg": "操作成功"
//            }
            if (resultMap != null && resultMap.get("code") != null && (Integer) resultMap.get("code") != 200) {
                log.error((String) resultMap.get("msg"));
            }
            if (resultMap != null && resultMap.get("code") != null && (Integer) resultMap.get("code") == 200) {
                isCanDownload = (Boolean) resultMap.get("data");
            }
        }else if(methodType.toUpperCase().equals(Constants.METHOD_POST)){
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization",Authorization);
//            ResponseEntity<Map> res = restTemplate.exchange(urlAll + Constants.QUE + params, HttpMethod.GET, new HttpEntity<>(null, headers),Map.class);
//            log.info("restTemplate get with selfDefine header: {}", res);
//            Map<String, Object> resultMap = (Map<String, Object>) res;
        }
        return isCanDownload;
    }

    public static boolean isOSLinux() {
        Properties prop = System.getProperties();
        String os = prop.getProperty("os.name");
        if (os != null && os.toLowerCase().indexOf("linux") > -1) {
            return true;
        } else {
            return false;
        }
    }
}
