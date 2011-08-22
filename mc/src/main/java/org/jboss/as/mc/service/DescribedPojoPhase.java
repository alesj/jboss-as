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
import org.jboss.as.mc.descriptor.ConfigVisitor;
import org.jboss.as.mc.descriptor.ConstructorConfig;
import org.jboss.as.mc.descriptor.DefaultConfigVisitor;
import org.jboss.as.mc.descriptor.ValueConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * MC pojo described phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DescribedPojoPhase implements Service<BeanInfo> {
    private final Module module;
    private final DeploymentReflectionIndex index;
    private final BeanMetaDataConfig beanConfig;
    private BeanInfo beanInfo;

    public DescribedPojoPhase(Module module, DeploymentReflectionIndex index, BeanMetaDataConfig beanConfig) {
        this.module = module;
        this.index = index;
        this.beanConfig = beanConfig;
    }

    public void start(StartContext context) throws StartException {
        try {
            Class beanClass = Class.forName(beanConfig.getBeanClass(), false, module.getClassLoader());
            beanInfo = new DefaultBeanInfo(index, beanClass);

            final ServiceName name = BeanMetaDataConfig.JBOSS_MC_POJO.append(beanConfig.getName()).append(BeanState.INSTANTIATED.name());
            final InstantiatedPojoPhase instantiatedPhase = new InstantiatedPojoPhase();
            final ServiceTarget serviceTarget = context.getChildTarget();
            final ServiceBuilder serviceBuilder = serviceTarget.addService(name, instantiatedPhase);
            final ConfigVisitor visitor = new DefaultConfigVisitor(serviceBuilder, BeanState.DESCRIBED, module.getClassLoader());

            beanConfig.visit(visitor);

            Joinpoint instantiateJoinpoint;
            ConstructorConfig ctorConfig = beanConfig.getConstructor();
            if (ctorConfig != null) {
                String factoryMethod = ctorConfig.getFactoryMethod();
                if (factoryMethod == null)
                    throw new StartException("Missing factory method in ctor configuration: " + beanConfig);

                ValueConfig[] parameters = ctorConfig.getParameters();
                String[] types = Configurator.getTypes(parameters);

                String factoryClass = ctorConfig.getFactoryClass();
                if (factoryClass != null) {
                    Class<?> factoryClazz = Class.forName(factoryClass, false, module.getClassLoader());
                    Method method = Configurator.findMethodInfo(index, factoryClazz, factoryMethod, types, true, true, true);
                    MethodJoinpoint mj = new MethodJoinpoint(method);
                    mj.setTarget(new ImmediateValue<Object>(null)); // null, since this is static call
                    mj.setParameters(parameters);
                    instantiateJoinpoint = mj;
                } else {
                    ValueConfig factory = ctorConfig.getFactory();
                    if (factory == null)
                        throw new StartException("Missing factoy value: " + beanConfig);

                    ReflectionJoinpoint rj = new ReflectionJoinpoint(index, factoryMethod, types);
                    rj.setTarget(factory);
                    rj.setParameters(parameters);
                    instantiateJoinpoint = rj;
                }
            } else {
                Constructor ctor = beanInfo.getConstructor();
                instantiateJoinpoint = new ConstructorJoinpoint(ctor);
            }
            // set bean config, joinpoint & install
            instantiatedPhase.setModule(module);
            instantiatedPhase.setBeanConfig(beanConfig);
            instantiatedPhase.setBeanInfo(beanInfo);
            instantiatedPhase.setInstantiationJoinpoint(instantiateJoinpoint);
            serviceBuilder.install();
        } catch (StartException e) {
            throw e;
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    public void stop(StopContext context) {
        // TODO
    }

    public BeanInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return beanInfo;
    }
}
