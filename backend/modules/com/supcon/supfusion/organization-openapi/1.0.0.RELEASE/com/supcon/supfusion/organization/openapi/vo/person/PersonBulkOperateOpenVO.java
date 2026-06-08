package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonBulkOperateOpenVO extends VO {

    /**
     * 新增的人员
     */
    private List<PersonAddOpenVO> addPersons;

    /**
     * 修改的人员
     */
    private List<PersonUpdateOpenVO> updatePersons;

    /**
     * 删除的人员编号
     */
    private List<String> deletePersons;
}
