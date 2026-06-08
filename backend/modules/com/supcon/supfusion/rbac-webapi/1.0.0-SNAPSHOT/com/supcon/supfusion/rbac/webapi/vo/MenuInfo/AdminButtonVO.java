package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Setter
@Getter
@ToString
public class AdminButtonVO extends VO {

    private static final long serialVersionUID = 1L;

    private boolean show;

}
