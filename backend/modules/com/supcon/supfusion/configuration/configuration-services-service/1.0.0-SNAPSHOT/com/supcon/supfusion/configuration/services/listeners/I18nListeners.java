package com.supcon.supfusion.configuration.services.listeners;

import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.listeners.events.I18nEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


/**
 * @Description: 国际化监听类
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/7
 */
@Component
public class I18nListeners implements ApplicationListener<I18nEvent> {

    @Autowired
    private InternationalService internationalService;

    @Override
    public void onApplicationEvent(I18nEvent event) {
        internationalService.addInternational(event.getI18n());
    }
}
