package com.supcon.supfusion.auth.api.dto;


import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonRoleDTO extends DTO {

    private Long personId;

    private String personCode;

    private String personName;

    private Set<Long> roleId;


}
