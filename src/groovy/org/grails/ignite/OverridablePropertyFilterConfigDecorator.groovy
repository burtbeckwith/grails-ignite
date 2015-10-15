package org.grails.ignite

import javax.servlet.FilterConfig
import javax.servlet.ServletContext

/**
 * A simple decorator over a FilterConfig object that permits overriding of init parameters so that we can pull
 * values from the Grails Config
 *
 * @author dstieglitz
 */
class OverridablePropertyFilterConfigDecorator implements FilterConfig {

    private FilterConfig underlyingFilterConfig
    private Map overridedInitParameters = [:]

    OverridablePropertyFilterConfigDecorator(FilterConfig config) {
        underlyingFilterConfig = config
    }

    @Override
    String getFilterName() {
        return underlyingFilterConfig.filterName
    }

    @Override
    ServletContext getServletContext() {
        return underlyingFilterConfig.servletContext
    }

    @Override
    String getInitParameter(String name) {
        if (overridedInitParameters.containsKey(name)) {
            return overridedInitParameters[name]
        }
        else {
            return underlyingFilterConfig.getInitParameter(name)
        }
    }

    void overrideInitParameter(String name, String value) {
        overridedInitParameters[name] = value
    }

    @Override
    Enumeration<String> getInitParameterNames() {
        return underlyingFilterConfig.initParameterNames
    }
}
