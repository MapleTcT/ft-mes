/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年12月2日 下午2:35:20
 */
@Data
@AllArgsConstructor
public class OodmSettingDTO {
    
    private String templateNamespace;
    
    private String templateName;
    
    private String instanceName;
    
    private String serviceNamespace;
    
    private String serviceName;
}
