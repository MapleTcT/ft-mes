package com.supcon.supfusion.systemconfig.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogsDTO extends VO {

    @Max(value = 2,message = Constants.CATALOG_APP_TYPE)
    @Min(value = 1,message = Constants.CATALOG_SYSTEM_TYPE)
    private Integer type;

    @Valid
    @NotNull(message = Constants.CATALOG_IS_NULL)
    @Size(min = 1,message = Constants.CATALOG_LIST)
    private List<CatalogDTO> catalogs;
}
