package com.supcon.orchid.LIMSBasic.utils;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;
import com.supcon.orchid.LIMSBasic.daos.LIMSBasicPickSiteDao;
import com.supcon.orchid.ec.entities.Module;
import com.supcon.orchid.ec.services.ModelServiceFoundation;
import com.supcon.orchid.ec.services.MsModuleRelationService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.services.BaseServiceImpl;
import com.supcon.orchid.support.Result;

@Component
@Transactional
public class ServiceClientUtils extends BaseServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ServiceClientUtils.class);

    @Autowired
    private LIMSBasicPickSiteDao pickSiteDao;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private MsModuleRelationService msModuleRelationService;

    @Autowired
    private ModelServiceFoundation modelServiceFoundation;

    public ServiceInstance getServiceInstance(String moduleCode) {
        Result moduleResult = judgeModule(moduleCode);
        if (!moduleResult.isSuccess(moduleResult)) {
            throw new BAPException(moduleResult.getMessage());
        }
        return loadBalancerClient.choose(moduleCode);
    }

    @Transactional(readOnly = true)
    public Result judgeModule(String moduleCode) {
        if (StringUtils.isEmpty(moduleCode)) {
            String info = InternationalResource.get(
                "LIMSBasic.HttpClientUtils.moduleIsEmpty",
                getCurrentUser().getLanguage()
            );
            return Result.fail(info);
        }

        String judgeUploadModuleSql =
            "SELECT CODE FROM EC_MODULE WHERE valid=1 and ARTIFACT = '" + moduleCode + "'";
        List<Object> codeList = pickSiteDao.createNativeQuery(judgeUploadModuleSql).list();
        if (codeList.isEmpty()) {
            String info = InternationalResource.get(
                "LIMSBasic.HttpClientUtils.moduleIsNotUpload",
                getCurrentUser().getLanguage(),
                moduleCode
            );
            return Result.fail(info);
        }

        String code = codeList.get(0).toString();
        Module module = modelServiceFoundation.getModule(code);
        if (module == null) {
            String info = InternationalResource.get(
                "LIMSBasic.HttpClientUtils.moduleIsNotPublish",
                getCurrentUser().getLanguage(),
                moduleCode
            );
            return Result.fail(info);
        }

        log.info("Artifact:" + module.getArtifact());
        if (!msModuleRelationService.checkModuleStatus(module.getArtifact())) {
            String info = InternationalResource.get(
                "LIMSBasic.HttpClientUtils.moduleIsNotPublish",
                getCurrentUser().getLanguage(),
                moduleCode
            );
            return Result.fail(info);
        }
        return Result.success("");
    }

    public void convertHeaders(HttpHeaders httpHeaders) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            httpHeaders.set(name, request.getHeader(name));
        }
    }

    @Transactional(readOnly = true)
    public String getWomSourceCode() {
        ServiceInstance serviceInstance = getServiceInstance("WOM");
        String baseUrl = String.format(
            "http://%s:%s",
            serviceInstance.getHost(),
            serviceInstance.getPort() + "/WOM/quality/quality/getWomSource"
        );
        HttpHeaders httpHeaders = new HttpHeaders();
        convertHeaders(httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            new HttpEntity<Object>(httpHeaders),
            String.class
        );
        JSONObject envelope = JSONObject.parseObject(responseEntity.getBody());
        Object responseCode = envelope.get("code");
        boolean success = Boolean.TRUE.equals(envelope.getBoolean("success"))
            || "200".equals(String.valueOf(responseCode));
        if (!success) {
            return "";
        }

        Object rawData = envelope.get("data");
        JSONObject data = null;
        if (rawData instanceof JSONObject) {
            data = (JSONObject) rawData;
        } else if (rawData instanceof String && StringUtils.isNotBlank((String) rawData)) {
            data = JSONObject.parseObject((String) rawData);
        }
        return data == null ? "" : StringUtils.defaultString(data.getString("code"));
    }
}
