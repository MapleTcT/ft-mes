package com.supcon.supfusion.organization.service.bo.group;

import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 岗位人员关系BO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupPersonBO {
    /**
     * 岗位ｉｄ
     */
    @NotNull(message = Constants.GROUP_PARAM_ID_NECESSARY)
    private Long groupId;
    /**
     * 关联人员id
     */
    private List<Long> persons;
}
