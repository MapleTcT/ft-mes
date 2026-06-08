package com.supcon.supfusion.systemconfig.common.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigsVo {
    List<ConfigVo> config;
}
