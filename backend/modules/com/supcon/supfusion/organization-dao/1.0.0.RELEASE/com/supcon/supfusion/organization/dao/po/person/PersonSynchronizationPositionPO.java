package com.supcon.supfusion.organization.dao.po.person;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonSynchronizationPositionPO {

    /**
     * 人员id
     */
    private Long personId;

    /**
     * 岗位编码
     */
    private String positionCode;

    /**
     * 岗位名称
     */
    private String positionName;
}
