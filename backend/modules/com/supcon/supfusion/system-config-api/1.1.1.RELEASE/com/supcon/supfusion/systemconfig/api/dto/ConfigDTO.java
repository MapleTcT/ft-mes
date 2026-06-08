package com.supcon.supfusion.systemconfig.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;


/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Valid
public class ConfigDTO extends VO {

    @NotEmpty(message = Constants.CONFIG_CODE)
    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> defaultValue;

    @NotEmpty(message = Constants.CONFIG_NAME)
    private String name;

    @Valid
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TypeConfig typeConfig;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Verify> verify;

    @Positive(message = Constants.CONFIG_ORDER)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double order;

    @Min(value = -1, message = Constants.CONFIG_TYPE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long configId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String extend;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Valid
    public static class TypeConfig {

        private String timeFormat;
        private Boolean isMore;

        @Valid
        private List<OptionalValue> optionalValue;

        private String remind;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Valid
        public static class OptionalValue {

            @NotEmpty(message = Constants.OPTIONALVALUE_LABEL)
            private String label;

            @NotEmpty(message = Constants.OPTIONALVALUE_VALUE)
            private String value;

            @NotNull(message = Constants.OPTIONALVALUE_ORDER)
            private Double order;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Verify {

        private Integer max;

        private Integer min;

        private String rex;

        private Boolean isRequire;

        private Boolean isNumber;

        private String msg;

        private Integer length;
    }

}
