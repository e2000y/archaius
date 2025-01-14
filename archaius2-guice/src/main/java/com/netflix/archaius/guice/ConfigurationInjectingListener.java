package com.netflix.archaius.guice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.netflix.archaius.ConfigMapper;
import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigLoader;
import com.netflix.archaius.api.IoCContainer;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.ConfigurationSource;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.exceptions.ConfigException;
import com.netflix.archaius.api.inject.LibrariesLayer;
import com.netflix.archaius.cascade.NoCascadeStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class ConfigurationInjectingListener implements ProvisionListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInjectingListener.class);
    
    @Inject
    private Config            config;
    
    @Inject
    private Injector          injector;
    
    @Inject
    private ConfigLoader      loader;
    
    @Inject
    private @LibrariesLayer   CompositeConfig   libraries;
    
    @com.google.inject.Inject(optional = true)
    private CascadeStrategy   cascadeStrategy;
    
    @Inject
    public static void init(ConfigurationInjectingListener listener) {
        LOG.info("Initializing ConfigurationInjectingListener");
    }
    
    CascadeStrategy getCascadeStrategy() {
        return cascadeStrategy != null ? cascadeStrategy : NoCascadeStrategy.INSTANCE;
    }
    
    private ConfigMapper mapper = new ConfigMapper();
    
    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        Class<?> clazz = provision.getBinding().getKey().getTypeLiteral().getRawType();
        
        //
        // Configuration Loading
        //
        final ConfigurationSource source = clazz.getDeclaredAnnotation(ConfigurationSource.class);
        if (source != null) {
            if (injector == null) {
                LOG.warn("Can't inject configuration into {} until ConfigurationInjectingListener has been initialized", clazz.getName());
                return;
            }
            
            CascadeStrategy strategy = source.cascading() != ConfigurationSource.NullCascadeStrategy.class
                    ? injector.getInstance(source.cascading()) : getCascadeStrategy();

            List<String> sources = Arrays.asList(source.value());
            Collections.reverse(sources);
            for (String resourceName : sources) {
                LOG.debug("Trying to loading configuration resource {}", resourceName);
                try {
                    CompositeConfig loadedConfig = loader
                        .newLoader()
                        .withCascadeStrategy(strategy)
                        .load(resourceName);
                    libraries.addConfig(resourceName, loadedConfig);
                } catch (ConfigException e) {
                    throw new ProvisionException("Unable to load configuration for " + resourceName, e);
                }
            }
        }
        
        //
        // Configuration binding
        //
        Configuration configAnnot = clazz.getAnnotation(Configuration.class);
        if (configAnnot != null) {
            if (injector == null) {
                LOG.warn("Can't inject configuration into {} until ConfigurationInjectingListener has been initialized", clazz.getName());
                return;
            }
            
            try {
                mapper.mapConfig(provision.provision(), config, new IoCContainer() {
                    @Override
                    public <S> S getInstance(String name, Class<S> type) {
                        return injector.getInstance(Key.get(type, Names.named(name)));
                    }

                    @Override
                    public boolean doInject(Field  field) {
                        return ((field.getAnnotation(jakarta.inject.Inject.class) != null) || (field.getAnnotation(com.google.inject.Inject.class) != null));
                    }

                    @Override
                    public boolean doInject(Method  method) {
                        return ((method.getAnnotation(jakarta.inject.Inject.class) != null) || (method.getAnnotation(com.google.inject.Inject.class) != null));
                    }
                });
            }
            catch (Exception e) {
                throw new ProvisionException("Unable to bind configuration to " + clazz, e);
            }
        }        
    }
}
