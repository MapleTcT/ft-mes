package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.Entity;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface EntityService {
    List<Entity> findEntities(String code);

}
