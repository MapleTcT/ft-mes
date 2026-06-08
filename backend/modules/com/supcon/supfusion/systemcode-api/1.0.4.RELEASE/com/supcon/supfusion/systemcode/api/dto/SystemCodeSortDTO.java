package com.supcon.supfusion.systemcode.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
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
public class SystemCodeSortDTO extends VO {

    private static final long serialVersionUID = 7026787783272102112L;

    private String parentId;

    private String parentName;

    private String prevId;

    private String nextId;

    private String currentId;
}
