package com.supcon.supfusion.organization.service;

import java.util.List;

/**
 * 助记码
 */
public interface OrgMnecodeService {

    /**
     * 增加助记码
     *
     * @param orgId        公司id、部门id、岗位id、人员id
     * @param orgMneSource 助记码元数据字段
     * @param option       类型
     */
    void addOrgMnecode(Long orgId, String orgMneSource, String option);

    /**
     * 删除助记码
     *
     * @param orgId  公司id、部门id、岗位id、人员id
     * @param option 类型
     */
    void deleteOrgMnecodeByOrgId(Long orgId, String option);

    /**
     * 批量删除删除助记码
     *
     * @param orgIds 公司id、部门id、岗位id、人员id
     * @param option 选项
     */
    void deleteOrgMnecodeByOrgId(List<Long> orgIds, String option);
}
