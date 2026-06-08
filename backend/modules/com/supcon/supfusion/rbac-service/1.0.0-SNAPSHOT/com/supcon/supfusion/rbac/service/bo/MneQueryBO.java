package com.supcon.supfusion.rbac.service.bo;

import com.supcon.supfusion.rbac.dao.po.TagPO;
import lombok.Data;

import static org.apache.poi.util.LocaleID.BO;

@Data
public class MneQueryBO{

    private String searchContent;

    private String type;

}
