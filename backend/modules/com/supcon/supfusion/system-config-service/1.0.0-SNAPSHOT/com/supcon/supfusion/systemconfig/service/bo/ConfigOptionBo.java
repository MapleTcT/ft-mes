package com.supcon.supfusion.systemconfig.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lifangyuan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigOptionBo {
    private Long id;
    private Long configId;
    private Double sort;
    private String label;
    private String selectValue;
}
