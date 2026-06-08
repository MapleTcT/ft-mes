package com.supcon.supfusion.portal.service.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class Module implements Serializable {

    private static final long serialVersionUID = 1464509948933913981L;
    private String code;
    private String name;
    private String nameInternational;
    private String category;//分类名

}
