package com.supcon.supfusion.systemconfig.common.client.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * @author lifangyuan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigVo {

    private String code;

    private JSONArray defaultValue;

    private String name;

    private TypeConfig typeConfig;

    private List<Verify> verify;

    private String order;

    private String type;
    private List<String> value;
    private String configId;
    private String extend;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeConfig {
        private String timeFormat;
        private Boolean isMore;
        private List<OptionalValue> optionalValue;
        private String remind;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OptionalValue {
            private String label;
            private String value;
            private String order;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Verify {
        private String max;
        private String min;
        private String rex;
        private Boolean isRequire;
        private Boolean isNumber;
        private String msg;
        private String length;
    }

}
