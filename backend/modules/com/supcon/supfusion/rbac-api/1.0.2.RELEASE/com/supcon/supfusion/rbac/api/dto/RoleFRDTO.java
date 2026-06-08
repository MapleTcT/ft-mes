package com.supcon.supfusion.rbac.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleFRDTO extends DTO {


    private static final long serialVersionUID = 3447280854653169600L;

    private Long id;

    private Integer fromPosition;
}
