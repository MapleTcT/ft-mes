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
public class PersonalThemeVO extends VO {

    private Long userId;

    private String theme;

    private Integer font;

    private Integer status;

    private String logo;
}
