package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.Event;

import java.util.List;
import java.util.Set;

/**
 * @author zhang yafei
 */
public interface EventService {
    List<Event> getEventsByLayoutCode(String layoutCode);

    Set<Event> getByFieldCode(String fieldCode);

    Set<Event> getByButtonCode(String buttonCode);
}
