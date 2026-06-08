package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

/**
 * @author zhang yafei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleVo extends VO {
    private static final long serialVersionUID = -6098923160888119136L;

    @ApiModelProperty("角色id")
    private Long id;

    @ApiModelProperty("角色code")
    private String code;

    @ApiModelProperty("角色名字")
    private String name;
}
