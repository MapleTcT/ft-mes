package com.supcon.supfusion.organization.webapi.vo.group;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 组分页获取
 *
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupPageVo extends VO {

    private Long companyId;
    private String name;
    private Integer current;
    private Integer pageSize;
}
