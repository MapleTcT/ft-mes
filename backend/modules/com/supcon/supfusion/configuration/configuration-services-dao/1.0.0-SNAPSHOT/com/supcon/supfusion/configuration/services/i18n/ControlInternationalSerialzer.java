package com.supcon.supfusion.configuration.services.i18n;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.supcon.supfusion.base.services.InternationalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/14
 */
@Slf4j
@Component
public class ControlInternationalSerialzer extends JsonSerializer<String> {

    @Autowired
    private InternationalService internationalService;
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) {
        try {
            gen.writeString(value);
            gen.writeStringField("controlNameInternational", internationalService.getI18nValue(value));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
