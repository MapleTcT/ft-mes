package com.supcon.supfusion.organization.dao.po.person;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonSynchronizationCompanyPO {

    /**
     * 人员id
     */
    private Long personId;

    /**
     * 公司编码
     */
    private String companyCode;

    /**
     * 公司全称
     */
    private String companyName;
}
