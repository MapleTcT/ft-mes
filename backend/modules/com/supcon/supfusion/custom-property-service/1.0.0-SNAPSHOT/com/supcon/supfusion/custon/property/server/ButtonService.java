package com.supcon.supfusion.custon.property.server;

import com.supcon.supfusion.custon.property.dao.entity.Button;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ButtonService {
    List<Button> getButtons(String code);

    List<Button> getButtonsByDataGridCode(String code);
}
