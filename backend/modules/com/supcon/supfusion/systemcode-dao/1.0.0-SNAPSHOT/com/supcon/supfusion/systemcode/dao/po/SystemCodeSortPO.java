package com.supcon.supfusion.systemcode.dao.po;

import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeSortPO  extends PO {

    private static final long serialVersionUID = 4249112669675705601L;

    private String parentId;

    private String parentName;

    private String prevId;

    private String nextId;

    private String currentId;
}
