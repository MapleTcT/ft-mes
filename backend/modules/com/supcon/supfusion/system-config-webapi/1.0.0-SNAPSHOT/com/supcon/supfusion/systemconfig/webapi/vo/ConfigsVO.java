package com.supcon.supfusion.systemconfig.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigsVO extends VO {
    List<ConfigVO> config;
}
