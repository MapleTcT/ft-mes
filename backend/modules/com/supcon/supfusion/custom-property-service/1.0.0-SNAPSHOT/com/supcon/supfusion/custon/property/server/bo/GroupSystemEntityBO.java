package com.supcon.supfusion.custon.property.server.bo;

import lombok.Data;

import java.util.List;

/**
 * @author zhang yafei
 */
@Data
public class GroupSystemEntityBO {
    private String name;
    private List<SystemEntityBO> list;
}
