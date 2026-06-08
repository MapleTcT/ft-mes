/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.i18n.InternationalResource
 *  freemarker.template.SimpleScalar
 *  freemarker.template.TemplateMethodModelEx
 */
package com.supcon.greendill.config.method;

import com.supcon.orchid.i18n.InternationalResource;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import java.util.List;

public class I18nMethodModel
implements TemplateMethodModelEx {
    public Object exec(List arguments) {
        SimpleScalar key;
        if (!arguments.isEmpty() && null != (key = (SimpleScalar)arguments.get(0))) {
            return new SimpleScalar(InternationalResource.get((String)key.toString()));
        }
        return null;
    }
}

