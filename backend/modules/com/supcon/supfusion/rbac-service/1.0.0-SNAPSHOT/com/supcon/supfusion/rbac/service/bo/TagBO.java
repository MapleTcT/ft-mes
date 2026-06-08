package com.supcon.supfusion.rbac.service.bo;

import com.supcon.supfusion.rbac.dao.po.TagPO;
import lombok.Data;

@Data
public class TagBO extends TagPO {

    private Long id;
    private String name;
}
