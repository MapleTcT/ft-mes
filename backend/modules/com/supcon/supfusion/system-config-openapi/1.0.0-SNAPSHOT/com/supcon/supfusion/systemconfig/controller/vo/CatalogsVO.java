package com.supcon.supfusion.systemconfig.controller.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogsVO extends VO {

    @Max(value = 2,message = Constants.CATALOG_APP_TYPE)
    @Min(value = 1,message = Constants.CATALOG_SYSTEM_TYPE)
    private Integer type;

    @Valid
    @NotNull(message = Constants.CATALOG_IS_NULL)
    @Size(min = 1,message = Constants.CATALOG_LIST)
    private List<CatalogVO> catalogs;
}
