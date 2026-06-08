package com.supcon.supfusion.notification.sms.service.runner;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.notification.sms.client.SuposClint;
import com.supcon.supfusion.notification.sms.config.SuposConfiguration;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * 初始化系统配置
 *
 * @author chenweinan
 * @create 2020/7/2
 */
@Component("smsJincangConfigRunner")
@Slf4j
@Order(value = 1)
public class ConfigRunner implements CommandLineRunner {

    @Autowired
    private SuposClint suposClint;
    @Autowired
    SuposConfiguration suposConfiguration;

    static final String appcode = "suposjincang";

    private static String wsdlUrl = "jdbc:sqlserver://127.0.0.1:1433;databaseName=supFusion;";
    private static String username = "username";
    private static String password = "password";

    @Override
    public void run(String... args) throws Exception {


        String request = JSONObject.toJSONString(new ConfigDto().setType(2)
                .setCatalogs(Arrays.asList(new ConfigDto.Catalog()
                        .setAppCode(suposConfiguration.getAppId())
                        .setName("短信")
                        .setOrder("1")
                        .setCode(appcode)
                        .setConfig(Arrays.asList(
                                new ConfigDto.Config()
                                        .setCode("url")
                                        .setName("url")
                                        .setType("0")
                                        .setOrder("1")
                                        .setDefaultValue(Arrays.asList(wsdlUrl))
                                        .setTypeConfig(new ConfigDto.TypeConfig().setRemind(""))
                                        .setVerify(Arrays.asList(new ConfigDto.Verify().setRequire(true)
                                                .setMax(50)

                                        )),
                                new ConfigDto.Config()
                                        .setCode("name")
                                        .setName("name")
                                        .setType("0")
                                        .setDefaultValue(Arrays.asList(username))
                                        .setOrder("2")
                                        .setTypeConfig(new ConfigDto.TypeConfig().setRemind(""))
                                        .setVerify(Arrays.asList(new ConfigDto.Verify().setRequire(true)
                                                .setMax(50)

                                        )),
                                new ConfigDto.Config()
                                        .setCode("password")
                                        .setName("password")
                                        .setDefaultValue(Arrays.asList(password))
                                        .setType("0")
                                        .setOrder("3")
                                        .setTypeConfig(new ConfigDto.TypeConfig().setRemind(""))
                                        .setVerify(Arrays.asList(new ConfigDto.Verify().setRequire(true)
                                                .setMax(50)

                                        )))

                        ))));


        try {

            String tenantId = RpcContext.getContext().getTenantId();
            log.info(">>> tenantId{} ", tenantId);
            suposClint.addSystemConfig(request);

        } catch (Exception e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        }

        log.info("-----初始化系统配置完成-----");

    }

    public Triple<String, String, String> getSmsConfig() {
        try {
            String systemConfig = suposClint.getSystemConfig(suposConfiguration.getAppId());
            log.info("soapclient << {} ",systemConfig);

            Configs configs = JSONObject.parseObject(systemConfig, Configs.class);

            if (Objects.equals(configs.getCode(), 100000000L)) {

                List<Configs.Config> config = Optional.ofNullable(configs.getData().getConfig()).orElse(Collections.emptyList());
                String wsdl = config.stream().filter(x -> x.getName().equals("wsdl")).map(x -> x.getValue().get(0)).findAny().orElse(wsdlUrl);
                String username = config.stream().filter(x -> x.getName().equals("name")).map(x -> x.getValue().get(0)).findAny().orElse(wsdlUrl);
                String password = config.stream().filter(x -> x.getName().equals("password")).map(x -> x.getValue().get(0)).findAny().orElse(wsdlUrl);


                Triple<String, String, String> configT3 = Triple.of(wsdl, username, password);
                log.info("configT3 = {}",configT3);
                return configT3;
            }

        } catch (Exception e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        }
        return null;
    }


    @Data
    @Accessors(chain = true)
    @SuppressWarnings("unused")
    static class ConfigDto {

        private List<Catalog> catalogs;
        private long type;

        @Data
        @Accessors(chain = true)
        @SuppressWarnings("unused")
        public static class Config {

            private String code;
            private List<String> defaultValue;
            private String name;
            private String order;
            private String type;
            private TypeConfig typeConfig;
            private List<Verify> verify;
        }

        @Data
        @Accessors(chain = true)
        @SuppressWarnings("unused")
        public static class Catalog {

            private String appCode;
            private String code;
            private List<Config> config;
            private String name;
            private String order;
        }

        @Data
        @Accessors(chain = true)
        @SuppressWarnings("unused")
        public static class TypeConfig {

            private String remind;

        }

        @Data
        @Accessors(chain = true)
        @SuppressWarnings("unused")
        public static class Verify {

            @JSONField(name = "isRequire")
            @JsonProperty("isRequire")
            private boolean isRequire;
            private Integer max;
        }
    }


}
