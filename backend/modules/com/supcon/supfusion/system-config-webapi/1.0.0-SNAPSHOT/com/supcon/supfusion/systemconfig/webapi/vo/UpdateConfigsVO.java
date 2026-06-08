package com.supcon.supfusion.systemconfig.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UpdateConfigsVO extends VO {

    private Long catalogId;

    private List<UpdateConfigVO> config;
}
