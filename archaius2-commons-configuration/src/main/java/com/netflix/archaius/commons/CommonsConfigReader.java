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

import java.net.URL;

import com.netflix.archaius.api.ConfigReader;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.exceptions.ConfigException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.ConfigurationException;

public class CommonsConfigReader implements ConfigReader {
    @Override
    public com.netflix.archaius.api.Config load(ClassLoader loader, String resourceName, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        try {
            return load(loader.getResource(resourceName));
        } catch (Exception e) {
            throw new ConfigException("Failed to load configuration from " + resourceName, e);
        }
    }

    @Override
    public com.netflix.archaius.api.Config load(ClassLoader loader, URL url, StrInterpolator strInterpolator, StrInterpolator.Lookup lookup) throws ConfigException {
        try {
            return load(url);
        } catch (Exception e) {
            throw new ConfigException("Failed to load configuration from " + url, e);
        }
    }

    private com.netflix.archaius.api.Config load(URL url) throws ConfigurationException {
        DefaultConfigurationBuilder  config = new DefaultConfigurationBuilder(url);

        config.setDefaultListDelimiter('|');
        
        return new CommonsToConfig(config.getConfiguration(true));
    }

    @Override
    public boolean canLoad(ClassLoader loader, String name) {
        return true;
    }

    @Override
    public boolean canLoad(ClassLoader loader, URL uri) {
        return true;
    }
}
