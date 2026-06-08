package com.supcon.supfusion.theme.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemThemeVO extends VO {

    private String theme;

    private String logo;

    private Integer font;

    private Integer status;
}
