package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.server.bo.GroupSystemEntityBO;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface SystemCodeService {
    List<GroupSystemEntityBO> getSystemEntityMapByGroup(String moduleCode);
}
