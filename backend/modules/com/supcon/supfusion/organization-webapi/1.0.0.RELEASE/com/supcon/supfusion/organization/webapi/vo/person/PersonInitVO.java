package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBaseBO;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 人员新增时初始化数据类信息
 *
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonInitVO extends VO {

    /**
     * 性别系统编码
     */
    private List<SystemCodeBaseBO> genders;

    /**
     * 涉密等级
     */
    private List<SystemCodeBaseBO> classifiedLevels;

    /**
     * 人员状态
     */
    private List<SystemCodeBaseBO> statuses;
}
