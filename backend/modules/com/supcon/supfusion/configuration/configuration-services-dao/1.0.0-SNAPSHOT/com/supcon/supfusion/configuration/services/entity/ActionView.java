package com.supcon.supfusion.configuration.services.entity;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Data
public class ActionView implements Serializable {
    private static final long serialVersionUID = -7436616994105114860L;

    @Id
    private String actionUrl;

    private String viewCode;

    private String viewName;

    public ActionView() {
        super();
    }

    public ActionView(String actionUrl, String viewCode, String viewName) {
        super();
        this.actionUrl = actionUrl;
        this.viewCode = viewCode;
        this.viewName = viewName;
    }

}
