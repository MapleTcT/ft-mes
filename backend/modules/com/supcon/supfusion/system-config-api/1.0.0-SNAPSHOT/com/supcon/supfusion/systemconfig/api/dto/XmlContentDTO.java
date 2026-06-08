package com.supcon.supfusion.systemconfig.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Valid
public class XmlContentDTO extends VO {

    @Valid
    @NotNull(message = Constants.XML_CONTENT_IS_NULL)
    @Size(min = 1, message = Constants.XML_CONTENT_LIST)
    List<String> xmlList;
}
