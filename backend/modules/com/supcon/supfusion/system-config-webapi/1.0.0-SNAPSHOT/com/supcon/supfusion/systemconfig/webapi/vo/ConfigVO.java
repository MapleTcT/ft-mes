package com.supcon.supfusion.systemconfig.webapi.vo;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConfigVO extends VO {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String code;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private JSONArray defaultValue;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private TypeConfig typeConfig;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Verify> verify;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer type;
    private List<String> value;
    private String configId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String extend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeConfig {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String timeFormat;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Boolean isMore;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<OptionalValue> optionalValue;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String tip;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OptionalValue {
            private String label;
            private String value;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Verify {

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Integer max;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Integer min;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String rex;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean isRequire;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean isNumber;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String msg;

    }

}
