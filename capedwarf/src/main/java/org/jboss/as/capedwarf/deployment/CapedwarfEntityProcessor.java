/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.ImmediateValue;

/**
 * Read entity classes for annotations; e.g. allocationSize.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfEntityProcessor extends CapedwarfDeploymentUnitProcessor {
    private static final DotName JPA_ENTITY = DotName.createSimple("javax.persistence.Entity");
    private static final DotName JPA_SEQUENCE_GENERATOR = DotName.createSimple("javax.persistence.SequenceGenerator");
    private static final DotName JDO_SEQUENCE = DotName.createSimple("javax.jdo.annotations.Sequence");

    private static JndiName JNDI_NAME = JndiName.of("java:jboss").append(CAPEDWARF).append("persistence").append("allocationsMap");

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final Map<String, Integer> allocationsMap = new HashMap<String, Integer>();
        // handle JPA
        final List<AnnotationInstance> entities = index.getAnnotations(JPA_ENTITY);
        if (entities.isEmpty() == false) {
            final List<AnnotationInstance> generators = index.getAnnotations(JPA_SEQUENCE_GENERATOR);
            if (generators.isEmpty() == false) {
                // fill-in entity classes
                final Map<String, AnnotationInstance> entityMap = new HashMap<String, AnnotationInstance>();
                for (AnnotationInstance ai : entities) {
                    final AnnotationTarget target = ai.target();
                    final ClassInfo ci = (ClassInfo) target;
                    entityMap.put(ci.name().local(), ai);
                }
                // map sequence generator to its entity
                for (AnnotationInstance ai : generators) {
                    final AnnotationValue allocationSize = ai.value("allocationSize");
                    if (allocationSize != null) {
                        AnnotationTarget target = ai.target();
                        String className = null;
                        if (target instanceof ClassInfo) {
                            final ClassInfo ci = (ClassInfo) target;
                            className = ci.name().local();
                        } else if (target instanceof MethodInfo) {
                            final MethodInfo mi = (MethodInfo) target;
                            className = mi.declaringClass().name().local();
                        } else if (target instanceof FieldInfo) {
                            final FieldInfo fi = (FieldInfo) target;
                            className = fi.declaringClass().name().local();
                        }

                        if (className != null) {
                            final Set<String> kinds = new HashSet<String>();
                            final AnnotationInstance entityAnnotation = entityMap.get(className);
                            if (entityAnnotation != null) {
                                kinds.add(toKind(className, entityAnnotation));
                            } else {
                                final Set<ClassInfo> allKnownSubclasses = index.getAllKnownSubclasses(DotName.createSimple(className));
                                for (ClassInfo ci : allKnownSubclasses) {
                                    final String ciCN = ci.name().local();
                                    final AnnotationInstance ea = entityMap.get(ciCN);
                                    if (ea != null) {
                                        kinds.add(toKind(ciCN, ea));
                                    }
                                }
                            }

                            for (String kind : kinds) {
                                allocationsMap.put(kind, allocationSize.asInt());
                            }
                        }
                    }
                }
            }
        }
        // handle JDO
        for (AnnotationInstance ai : index.getAnnotations(JDO_SEQUENCE)) {
            final AnnotationValue extensions = ai.value("extensions");
            if (extensions != null) {
                final String kind = toKind(((ClassInfo) ai.target()).name().local());
                final AnnotationInstance[] aies = extensions.asNestedArray();
                for (AnnotationInstance aie : aies) {
                    final AnnotationValue vendorName = aie.value("vendorName");
                    final AnnotationValue key = aie.value("key");
                    if (vendorName != null && key != null && "datanucleus".equals(vendorName.asString()) && "key-cache-size".equals(key.asString())) {
                        final AnnotationValue value = aie.value("value");
                        if (value != null) {
                            allocationsMap.put(kind, Integer.parseInt(value.asString()));
                        }
                    }
                }
            }
        }

        // push allocationsMap to JNDI
        final String jndiName = JNDI_NAME.append(CapedwarfDeploymentMarker.getAppId(unit)).getAbsoluteName();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binder = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<ManagedReferenceFactory> binderBuilder = phaseContext.getServiceTarget().addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addInjectionValue(new ManagedReferenceInjector<Map>(binder.getManagedObjectInjector()), new ImmediateValue<Map>(allocationsMap))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);
        binderBuilder.install();
    }

    private static String toKind(String className, AnnotationInstance entityAnnotation) {
        final AnnotationValue name = entityAnnotation.value("name");
        if (name != null && name.asString().length() > 0) {
            return name.asString();
        }

        return toKind(className);
    }

    private static String toKind(String className) {
        final int p = className.lastIndexOf(".");
        return (p > 0) ? className.substring(p) : className;
    }
}
