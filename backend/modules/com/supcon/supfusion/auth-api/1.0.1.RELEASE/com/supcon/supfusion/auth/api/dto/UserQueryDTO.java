package com.supcon.supfusion.auth.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryDTO extends DTO {
    private List<Long> personIds;
    private String keyword;
}
