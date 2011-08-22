/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mc.service;

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * MC pojo described phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DescribedPojoPhase extends AbstractPojoPhase {
    private final DeploymentReflectionIndex index;

    public DescribedPojoPhase(DeploymentReflectionIndex index, BeanMetaDataConfig beanConfig) {
        this.index = index;
        setBeanConfig(beanConfig);
    }

    @Override
    protected BeanState getLifecycleState() {
        return BeanState.DESCRIBED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new InstantiatedPojoPhase(index, this);
    }

    public void start(StartContext context) throws StartException {
        try {
            setModule(getBeanConfig().getModule().getInjectedModule().getValue());
            String beanClass = getBeanConfig().getBeanClass();
            if (beanClass != null) {
                Class clazz = Class.forName(beanClass, false, getModule().getClassLoader());
                setBeanInfo(new DefaultBeanInfo(index, clazz));
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
        super.start(context);
    }

    public BeanInfo getValue() throws IllegalStateException, IllegalArgumentException {
        BeanInfo beanInfo = getBeanInfo();
        if (beanInfo == null)
            throw new IllegalStateException("Cannot determine bean info at this state, try setting bean's class attribute.");
        return beanInfo;
    }
}
