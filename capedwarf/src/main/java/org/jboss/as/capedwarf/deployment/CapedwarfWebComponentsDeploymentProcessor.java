package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.capedwarf.appidentity.CDIListener;
import org.jboss.capedwarf.appidentity.GAEFilter;
import org.jboss.capedwarf.users.AuthServlet;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Add GAE filter and auth servlet.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfWebComponentsDeploymentProcessor extends CapedwarfWebModificationDeploymentProcessor {

    private static final String GAE_FILTER_NAME = "GAEFilter";
    private static final String AUTH_SERVLET_NAME = "authservlet";

    private final ListenerMetaData CDI_LISTENER;
    private final FilterMetaData GAE_FILTER;
    private final FilterMappingMetaData GAE_FILTER_MAPPING;
    private final JBossServletMetaData GAE_SERVLET;
    private final ServletMappingMetaData GAE_SERVLET_MAPPING;

    public CapedwarfWebComponentsDeploymentProcessor() {
        CDI_LISTENER = createListener();
        GAE_FILTER = createFilter();
        GAE_FILTER_MAPPING = createFilterMapping();
        GAE_SERVLET = createAuthServlet();
        GAE_SERVLET_MAPPING = createAuthServletMapping();
    }

    @Override
    protected void doDeploy(DeploymentUnit unit, JBossWebMetaData webMetaData, Type type) {
        if (type == Type.MERGED) {
            addListenerTo(webMetaData);

            addFilterTo(webMetaData);
            addFilterMappingTo(webMetaData);

            addAuthServletTo(webMetaData);
            addAuthServletMappingTo(webMetaData);
        }
    }

    private void addListenerTo(JBossWebMetaData webMetaData) {
        getListeners(webMetaData).add(CDI_LISTENER);
    }

    private ListenerMetaData createListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass(CDIListener.class.getName());
        return listener;
    }

    private List<ListenerMetaData> getListeners(JBossWebMetaData webMetaData) {
        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<ListenerMetaData>();
            webMetaData.setListeners(listeners);
        }
        return listeners;
    }

    private void addFilterTo(JBossWebMetaData webMetaData) {
        getFilters(webMetaData).add(GAE_FILTER);
    }

    private FilterMetaData createFilter() {
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterName(GAE_FILTER_NAME);
        filter.setFilterClass(GAEFilter.class.getName());
        return filter;
    }

    private FiltersMetaData getFilters(JBossWebMetaData webMetaData) {
        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        return filters;
    }

    private void addFilterMappingTo(JBossWebMetaData webMetaData) {
        getFilterMappings(webMetaData).add(GAE_FILTER_MAPPING);
    }

    private FilterMappingMetaData createFilterMapping() {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(GAE_FILTER_NAME);
        filterMapping.setUrlPatterns(Collections.singletonList("/*"));
        return filterMapping;
    }

    private List<FilterMappingMetaData> getFilterMappings(JBossWebMetaData webMetaData) {
        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings(filterMappings);
        }
        return filterMappings;
    }

    private void addAuthServletTo(JBossWebMetaData webMetaData) {
        getServlets(webMetaData).add(GAE_SERVLET);
    }

    private JBossServletsMetaData getServlets(JBossWebMetaData webMetaData) {
        JBossServletsMetaData servletsMetaData = webMetaData.getServlets();
        if (servletsMetaData == null) {
            servletsMetaData = new JBossServletsMetaData();
            webMetaData.setServlets(servletsMetaData);
        }
        return servletsMetaData;
    }

    private JBossServletMetaData createAuthServlet() {
        JBossServletMetaData servlet = new JBossServletMetaData();
        servlet.setServletName(AUTH_SERVLET_NAME);
        servlet.setServletClass(AuthServlet.class.getName());
        servlet.setEnabled(true);
        return servlet;
    }

    private void addAuthServletMappingTo(JBossWebMetaData webMetaData) {
        getServletMappings(webMetaData).add(GAE_SERVLET_MAPPING);
    }

    private List<ServletMappingMetaData> getServletMappings(JBossWebMetaData webMetaData) {
        List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
        if (servletMappings == null) {
            servletMappings = new ArrayList<ServletMappingMetaData>();
            webMetaData.setServletMappings(servletMappings);
        }
        return servletMappings;
    }

    private ServletMappingMetaData createAuthServletMapping() {
        ServletMappingMetaData servletMapping = new ServletMappingMetaData();
        servletMapping.setServletName(AUTH_SERVLET_NAME);
        servletMapping.setUrlPatterns(Collections.singletonList(AuthServlet.SERVLET_URI + "/*"));   // TODO: introduce AuthServlet.URL_PATTERN
        return servletMapping;
    }
}
