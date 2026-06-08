package com.supcon.supfusion.custon.property.common.i18n;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/14
 */
@Slf4j
@Component
public class NameInternationalSerialzer extends JsonSerializer<String> {

    @Autowired
    private InternationalResource internationalResource;
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) {
        try {
            gen.writeString(value);
            gen.writeStringField("nameInternational", internationalResource.getI18nValue(value));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
