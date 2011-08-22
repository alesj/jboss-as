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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.service.ReflectionJoinpoint;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.value.InjectedValue;

import java.util.Arrays;
import java.util.List;

/**
 * Value factory value.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ValueFactoryConfig extends ValueConfig {
    private static final long serialVersionUID = 1L;

    private String bean;
    private BeanState state;
    private String method;
    private ValueConfig[] parameters;

    private final transient InjectedValue<Object> value = new InjectedValue<Object>();

    public Object getValue(Class<?> type) {
        try {
            ReflectionJoinpoint joinpoint = new ReflectionJoinpoint(DeploymentReflectionIndex.create(), method);
            joinpoint.setTarget(value);
            joinpoint.setParameters(parameters);
            return joinpoint.dispatch();
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
    }

    @Override
    public void visit(ConfigVisitor visitor) {
        if (bean != null) {
            visitor.addDependency(bean, state, value);
        }
        super.visit(visitor);
    }

    @Override
    protected void addChildren(ConfigVisitor visitor, List<ConfigVisitorNode> nodes) {
        if (parameters != null) {
            nodes.addAll(Arrays.asList(parameters));
        }
    }

    public void setBean(String dependency) {
        this.bean = dependency;
    }

    public void setState(BeanState state) {
        this.state = state;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParameters(ValueConfig[] parameters) {
        this.parameters = parameters;
    }
}
