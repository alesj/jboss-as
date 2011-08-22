/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.msc.service.ServiceName;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * The legacy bean meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BeanMetaDataConfig implements Serializable, ConfigVisitorNode {
    private static final long serialVersionUID = 1L;

    /** Name prefix of all MC-style beans. */
    private static final ServiceName JBOSS_MC_POJO = ServiceName.JBOSS.append("mc", "pojo");

    /**
     * Get MC bean name.
     *
     * @param name the original bean name
     * @param state the state
     * @return bean service name
     */
    public static ServiceName toBeanName(String name, BeanState state) {
        if (state == null)
            state = BeanState.INSTALLED;

        return JBOSS_MC_POJO.append(name).append(state.name());
    }

    private String name;
    private String beanClass;
    private Set<String> aliases;
    private ModuleConfig module;
    private ConstructorConfig constructor;
    private Set<PropertyConfig> properties;
    private LifecycleConfig create;
    private LifecycleConfig start;
    private LifecycleConfig stop;
    private LifecycleConfig destroy;
    private List<InstallConfig> installs;
    private List<InstallConfig> uninstalls;
    private Set<DependsConfig> depends;

    @Override
    public void visit(ConfigVisitor visitor) {
        BeanState state = visitor.getState();
        if (module == null)
            module = new ModuleConfig();
        if (state == BeanState.NOT_INSTALLED)
            module.visit(visitor);
        if (constructor != null && state == BeanState.DESCRIBED)
            constructor.visit(visitor);
        if (properties != null && state == BeanState.INSTANTIATED) {
            for (PropertyConfig pc : properties)
                pc.visit(visitor);
        }
        if (create != null && state == BeanState.CONFIGURED) {
            create.visit(visitor);
        }
        if (destroy != null && state == BeanState.CONFIGURED) {
            destroy.visit(visitor);
        }
        if (start != null && state == BeanState.CREATE) {
            start.visit(visitor);
        }
        if (stop != null && state == BeanState.CREATE) {
            stop.visit(visitor);
        }
        if (installs != null) {
            for (InstallConfig ic : installs)
                ic.visit(visitor);
        }
        if (uninstalls != null) {
            for (InstallConfig ic : uninstalls)
                ic.visit(visitor);
        }
        if (depends != null) {
            for (DependsConfig dc : depends)
                dc.visit(visitor);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(String beanClass) {
        this.beanClass = beanClass;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public ModuleConfig getModule() {
        return module;
    }

    public void setModule(ModuleConfig module) {
        this.module = module;
    }

    public ConstructorConfig getConstructor() {
        return constructor;
    }

    public void setConstructor(ConstructorConfig constructor) {
        this.constructor = constructor;
    }

    public Set<PropertyConfig> getProperties() {
        return properties;
    }

    public void setProperties(Set<PropertyConfig> properties) {
        this.properties = properties;
    }

    public LifecycleConfig getCreate() {
        return create;
    }

    public void setCreate(LifecycleConfig create) {
        this.create = create;
    }

    public LifecycleConfig getStart() {
        return start;
    }

    public void setStart(LifecycleConfig start) {
        this.start = start;
    }

    public LifecycleConfig getStop() {
        return stop;
    }

    public void setStop(LifecycleConfig stop) {
        this.stop = stop;
    }

    public LifecycleConfig getDestroy() {
        return destroy;
    }

    public void setDestroy(LifecycleConfig destroy) {
        this.destroy = destroy;
    }

    public List<InstallConfig> getInstalls() {
        return installs;
    }

    public void setInstalls(List<InstallConfig> installs) {
        this.installs = installs;
    }

    public List<InstallConfig> getUninstalls() {
        return uninstalls;
    }

    public void setUninstalls(List<InstallConfig> uninstalls) {
        this.uninstalls = uninstalls;
    }

    public Set<DependsConfig> getDepends() {
        return depends;
    }

    public void setDepends(Set<DependsConfig> depends) {
        this.depends = depends;
    }
}