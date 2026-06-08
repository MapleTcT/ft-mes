package com.supcon.supfusion.base.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/9/3
 */
@Data
@AllArgsConstructor
public class Language implements Serializable {

    private static final long serialVersionUID = 4141403006087710813L;

    private String key;
    private Boolean isUsed = true;
    private String internationalKey;
    private String displayName;

}
