/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.commons;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.configuration2.Configuration;

import com.netflix.archaius.config.AbstractConfig;

/**
 * Adaptor to allow an Apache Commons Configuration AbstractConfig to be used
 * as an Archaius2 Config
 */
public class CommonsToConfig extends AbstractConfig {

    private final Configuration config;
    
    public CommonsToConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        return config.getProperty(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    public <T> List<T> getList(String key, Class<T> type) {
        List value = config.getList(key);
        if (value == null) {
            return notFound(key);
        }

        List<T> result = new ArrayList<T>();
        for (Object part : value) {
            if (type.isInstance(part)) {
                result.add((T)part);
            } else if (part instanceof String) {
                result.add(getDecoder().decode(type, (String) part));
            } else {
                throw new UnsupportedOperationException(
                        "Property values other than " + type.getCanonicalName() +" or String not supported");
            }
        }
        return result;
    }

    @Override
    public List getList(String key) {
        List value = config.getList(key);
        if (value == null) {
            return notFound(key);
        }
        return value;
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = config.getString(key, defaultValue);

        return getStrInterpolator().create(getLookup()).resolve(value);
    }

    @Override
    public String getString(String key) {
        String value = config.getString(key);
        if (value == null) {
            return notFound(key);
        }
        return getStrInterpolator().create(getLookup()).resolve(value);
    }
}
