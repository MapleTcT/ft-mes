package com.supcon.supfusion.systemconfig.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
public class CatalogDTO extends VO {

    @NotEmpty(message = Constants.CATALOG_CODE)
    private String code;

    @Valid
    private List<ConfigDTO> config;

    @NotEmpty(message = Constants.CATALOG_NAME)
    private String name;

    @NotEmpty(message = Constants.CATALOG_APPCODE)
    private String appCode;

    @Positive(message = Constants.CATALOG_ORDER)
    private Double order;

    private Boolean hide = false;

}
