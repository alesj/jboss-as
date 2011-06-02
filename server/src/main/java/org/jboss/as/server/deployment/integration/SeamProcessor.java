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

package org.jboss.as.server.deployment.integration;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;

/**
 * Recognize Seam deployments and add org.jboss.seam.int module to it.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SeamProcessor implements DeploymentUnitProcessor {

    public static final String SEAM_PROPERTIES = "seam.properties";
    public static final String SEAM_PROPERTIES_META_INF = "META-INF/" + SEAM_PROPERTIES;
    public static final String SEAM_PROPERTIES_WEB_INF = "WEB-INF/classes/" + SEAM_PROPERTIES;
    public static final String SEAM_COMPONENTS = "components.xml";
    public static final String SEAM_COMPONENTS_META_INF = "META-INF/" + SEAM_COMPONENTS;
    public static final String SEAM_COMPONENTS_WEB_INF = "WEB-INF/classes/META-INF/" + SEAM_COMPONENTS;

    public static final String[] SEAM_FILES = new String[]{
            SEAM_PROPERTIES,
            SEAM_PROPERTIES_META_INF,
            SEAM_PROPERTIES_WEB_INF,
            SEAM_COMPONENTS_META_INF,
            SEAM_COMPONENTS_WEB_INF
    };

    public static final ModuleIdentifier SEAM_INT_MODULE = ModuleIdentifier.create("org.jboss.seam.int");
    public static final AttachmentKey<Boolean> ADDED = AttachmentKey.create(Boolean.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        DeploymentUnit top = DeploymentUtils.getTopDeploymentUnit(unit);
        if (top.hasAttachment(ADDED))
            return;

        final ResourceRoot mainRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (mainRoot == null)
            return;

        VirtualFile root = mainRoot.getRoot();
        for (String path : SEAM_FILES) {
            if (root.getChild(path).exists()) {
                final ModuleSpecification moduleSpecification = top.getAttachment(Attachments.MODULE_SPECIFICATION);
                final ModuleLoader moduleLoader = Module.getBootModuleLoader();
                moduleSpecification.addDependency(new ModuleDependency(moduleLoader, SEAM_INT_MODULE, false, false, true));
                top.putAttachment(ADDED, true);
                break;
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
