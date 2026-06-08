package com.supcon.supfusion.auth.openapi.suposvo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class DeleteRolesVO extends VO {

    private List<String> list;
}
