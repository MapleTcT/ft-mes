package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonBulkOperateOpenBO {

    /**
     * 新增的人员
     */
    private List<PersonAddOpenBO> addPersons;

    /**
     * 修改的人员
     */
    private List<PersonUpdateOpenBO> updatePersons;

    /**
     * 删除的人员编号
     */
    private List<String> deletePersons;
}
