package org.springframework.kafka.security.jaas;

import java.util.Map;

public class KafkaJaasLoginModuleInitializer {
  public static final String KAFKA_CLIENT_CONTEXT_NAME = "KafkaClient";

  public enum ControlFlag {
    OPTIONAL,
    REQUIRED,
    REQUISITE,
    SUFFICIENT
  }

  public KafkaJaasLoginModuleInitializer() {
  }

  public void setLoginModule(String loginModule) {
  }

  public void setControlFlag(ControlFlag controlFlag) {
  }

  public void setOptions(Map<String, String> options) {
  }

  public void afterSingletonsInstantiated() {
  }

  public void destroy() {
  }
}
