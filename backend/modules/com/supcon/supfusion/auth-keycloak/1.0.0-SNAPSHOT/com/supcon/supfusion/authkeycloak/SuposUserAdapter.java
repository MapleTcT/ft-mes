/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supcon.supfusion.authkeycloak;

import com.supcon.supfusion.authkeycloak.entity.UserEntity;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JBossLog
public class SuposUserAdapter extends AbstractUserAdapter {
    protected UserEntity entity;
    protected String keycloakId;

    public SuposUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, String.valueOf(entity.getUserName()));
    }


    public String getPassword() {
        return entity.getPassword();
    }

    public void setPassword(String password) {
        entity.setPassword(password);
    }

    @Override
    public String getUsername() {
        return entity.getUserName();
    }

    @Override
    public void setUsername(String username) {
        entity.setUserName(username);

    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public String getFirstAttribute(String name) {
        try {
            Field field = entity.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value != null) {
                return value.toString();
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        LinkedHashMap<String, List<String>> attributes = new LinkedHashMap<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    attributes.put(field.getName(), Collections.singletonList(value.toString()));
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return attributes;
    }

    @Override
    public List<String> getAttribute(String name) {
        String value = getFirstAttribute(name);
        if (value != null) {
            return Collections.singletonList(value);
        }
        return Collections.emptyList();
    }

    public UserEntity getEntity() {
        return entity;
    }

    public void setEntity(UserEntity entity) {
        this.entity = entity;
    }
}
