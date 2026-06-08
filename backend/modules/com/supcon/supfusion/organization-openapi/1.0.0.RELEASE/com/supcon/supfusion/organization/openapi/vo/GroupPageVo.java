package com.supcon.supfusion.organization.openapi.vo;


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
    private Integer nextPage;
    private Integer pageCount;
}
