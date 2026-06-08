
package com.supcon.supfusion.notification.sms.service.runner;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@NoArgsConstructor
@lombok.Data
@Accessors(chain = true)

public class Configs {

    private long code;
    private Data data;
    private String message;

    @NoArgsConstructor
    @lombok.Data
    @Accessors(chain = true)
    public static class Data {

        private List<Config> config;

    }

    @NoArgsConstructor
    @lombok.Data
    @Accessors(chain = true)
    public static class Config {

        private String code;
        private long configId;
        private String name;
        private List<String> value;

    }
}
