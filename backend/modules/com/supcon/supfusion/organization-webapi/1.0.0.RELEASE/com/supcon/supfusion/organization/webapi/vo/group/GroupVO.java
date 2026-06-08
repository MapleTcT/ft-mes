package com.supcon.supfusion.organization.webapi.vo.group;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 组列表信息
 *
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO extends VO {

    private Long id;
    private String name;
}
