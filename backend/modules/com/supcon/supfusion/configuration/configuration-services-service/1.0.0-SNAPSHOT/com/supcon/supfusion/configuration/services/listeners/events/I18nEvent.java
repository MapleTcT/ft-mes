package com.supcon.supfusion.configuration.services.listeners.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/7
 */
@Getter
@Setter
public class I18nEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public I18nEvent(Object source, String i18n) {
        super(source);
        this.i18n = i18n;
    }

    private String i18n;
}
