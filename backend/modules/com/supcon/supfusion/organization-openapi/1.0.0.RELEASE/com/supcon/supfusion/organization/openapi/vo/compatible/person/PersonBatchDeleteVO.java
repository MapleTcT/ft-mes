/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.organization.openapi.vo.compatible.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author: zhuangmh
 * @date: 2020年7月4日 上午11:18:54
 */
@Data
public class PersonBatchDeleteVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 编号列表
     */
    @NotNull(message = Constants.PERSON_PARAM_CODE_NECESSARY)
    private List<String> list;

}
