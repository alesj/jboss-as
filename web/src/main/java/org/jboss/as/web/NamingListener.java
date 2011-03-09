/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web;

import org.jboss.as.naming.context.NamespaceContextSelector;

/**
 * An InstanceListener used to push/pop the application naming context.
 *
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class NamingListener { // TODO -- add it as global event listener!

    private final NamespaceContextSelector selector;

    /**
     * Thread local used to initialise the Listener after startup.
     */
    private static final ThreadLocal<NamespaceContextSelector> localSelector = new ThreadLocal<NamespaceContextSelector>();

    public NamingListener() {
        selector = localSelector.get();
        if (selector == null)
            throw new IllegalArgumentException("Null selector");
    }

    /*
    public void instanceEvent(InstanceEvent event) {
        String type = event.getType();
        if (type.equals(InstanceEvent.BEFORE_DISPATCH_EVENT)
                || type.equals(InstanceEvent.BEFORE_REQUEST_EVENT)
                || type.equals(InstanceEvent.BEFORE_DESTROY_EVENT)
                || type.equals(InstanceEvent.BEFORE_INIT_EVENT)) {
            NamespaceContextSelector.pushCurrentSelector(selector);
        }
        else if (type.equals(InstanceEvent.AFTER_DISPATCH_EVENT)
                || type.equals(InstanceEvent.AFTER_REQUEST_EVENT)
                || type.equals(InstanceEvent.AFTER_DESTROY_EVENT)
                || type.equals(InstanceEvent.AFTER_INIT_EVENT)) {
            NamespaceContextSelector.popCurrentSelector();
        }
    } */

    public static void beginComponentStart(NamespaceContextSelector selector) {
        localSelector.set(selector);
        NamespaceContextSelector.pushCurrentSelector(selector);
    }

    public static void endComponentStart() {
        NamespaceContextSelector.popCurrentSelector();
        localSelector.set(null);
    }

}
