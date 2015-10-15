package org.grails.ignite

import grails.util.Holders

import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

import org.apache.ignite.cache.websession.WebSessionFilter as IgniteWebSessionFilter

/**
 * @author dstieglitz
 */
class WebSessionFilter extends IgniteWebSessionFilter {

    @Override
    void init(FilterConfig cfg) throws ServletException {
        // get grid name from application configuration
        OverridablePropertyFilterConfigDecorator decorator = new OverridablePropertyFilterConfigDecorator(cfg)

        def configuredGridName = IgniteStartupHelper.DEFAULT_GRID_NAME
        def conf = Holders.grailsApplication.config.ignite

        if (conf.containsKey('gridName')) {
            configuredGridName = conf.gridName
        }

        decorator.overrideInitParameter('IgniteWebSessionsGridName', configuredGridName)

        if (webSessionClusteringEnabled(conf)) {
            log.info "configuring web session clustering for gridName=${configuredGridName}"
            super.init(decorator)
        }
    }

    @Override
    void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (webSessionClusteringEnabled(Holders.grailsApplication.config.ignite)) {
            super.doFilter(req, res, chain)
        } else {
            chain.doFilter(req, res)
        }
    }

    private boolean webSessionClusteringEnabled(ConfigObject conf) {
        (conf.webSessionClusteringEnabled instanceof Boolean) && conf.webSessionClusteringEnabled
    }
}
