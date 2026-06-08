package com.supcon.supfusion.systemconfig.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigAndVersionDTO extends VO {
    public Boolean isUpdate;
    public ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> configMap;
    public ConcurrentHashMap<String, String> versionMap;
}
