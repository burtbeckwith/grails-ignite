package org.grails.ignite

import grails.spring.BeanBuilder
import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.ignite.Ignite
import org.apache.ignite.IgniteCheckedException
import org.apache.ignite.configuration.CacheConfiguration
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException
import org.springframework.context.ApplicationContext

/**
 * @author dstieglitz
 */
@Log4j
class IgniteStartupHelper {

    static final String IGNITE_WEB_SESSION_CACHE_NAME = 'session-cache'
    static final String DEFAULT_GRID_NAME = 'grid'
    static final String IGNITE_CONFIG_DIRECTORY_NAME = 'ignite'

    private static ApplicationContext igniteApplicationContext
    public static Ignite grid
    private static BeanBuilder igniteBeans = new BeanBuilder()

    static BeanBuilder getBeans(String resourcePattern, BeanBuilder bb = null) {
        if (bb == null) {
            bb = new BeanBuilder()
        }

        bb.classLoader = getClass().classLoader
        bb.binding = new Binding(application: Holders.grailsApplication)

//        def pluginDir = GrailsPluginUtils.pluginInfos.find { it.name == 'ignite' }?.pluginDir
//        def defaultUrl
//        def url
//
//        if (pluginDir != null) {
//            defaultUrl = "file:${pluginDir}/grails-app/conf/spring/${fileName}.groovy"
//            url = "file:grails-app/conf/spring/${fileName}"
//            log.info "loading default configuration from ${defaultUrl}"
//            bb.importBeans(defaultUrl)
//        } else {
//            url = "classpath*:${fileName}*"
//        }
//
//        log.info "attempting to load beans from ${url}"
//        bb.importBeans(url)

        bb.importBeans(resourcePattern)

        return bb
    }

    static boolean startIgnite() {
        // look for a IgniteResources.groovy file on the classpath
        // load it into an igniteApplicationContext and start ignite
        // merge the application context

        def conf = Holders.grailsApplication.config.ignite
        boolean igniteEnabled = (conf.enabled instanceof Boolean) && conf.enabled

        if (!conf.config.containsKey('locations')) {
            throw new IllegalArgumentException("You must specify the locations to Ignite configuration files in ignite.config.locations, see docs")
        }

        def locations = conf.config.locations

        if (!(locations instanceof Collection)) {
            throw new IllegalArgumentException("You must specify a collection of resource locations to Ignite configuration files in ignite.config.locations, see docs")
        }

        if (!locations) {
            throw new IllegalArgumentException("You must specify the locations to Ignite configuration files in ignite.config.locations, see docs")
        }

        log.debug "startIgnite() --> igniteEnabled=${igniteEnabled}"

        if (!igniteEnabled) {
            log.warn "startIgnite called, but ignite is not enabled in configuration"
            return false
        }

        locations.each {
            log.info "loading Ignite beans configuration from ${it}"
            getBeans(it, igniteBeans)
        }

        igniteApplicationContext = igniteBeans.createApplicationContext()
        if (igniteApplicationContext == null) {
            throw new IllegalArgumentException("Unable to initialize")
        }

        igniteApplicationContext.beanDefinitionNames.each {
            log.debug "found bean ${it}"
        }

        return startIgniteFromSpring()
    }

    static boolean startIgniteFromSpring() {
        def ctx = igniteApplicationContext

        def configuredGridName = DEFAULT_GRID_NAME
        def conf = Holders.grailsApplication.config.ignite
        if (conf.containsKey('gridName')) {
            configuredGridName = conf.gridName
        }

        System.setProperty("IGNITE_QUIET", "false")

        BeanBuilder cacheBeans

        try {
            log.info "looking for cache resources..."
            cacheBeans = getBeans("IgniteCacheResources")
            log.debug "found ${cacheBeans} cache resources"
        } catch (BeanDefinitionParsingException e) {
            log.error e.message
            log.warn "No cache configuration found or cache configuration could not be loaded"
        }

        try {
            grid = ctx.getBean('grid')

            def cacheConfigurationBeans = []

            if (cacheBeans != null) {
                ApplicationContext cacheCtx = cacheBeans.createApplicationContext()
                log.info "found ${cacheCtx.beanDefinitionCount} cache resource beans"
                cacheCtx.beanDefinitionNames.each { beanDefName ->
                    def bean = cacheCtx.getBean(beanDefName)
                    if (bean instanceof CacheConfiguration) {
                        log.info "found manually-configured cache bean ${beanDefName}"
                        cacheConfigurationBeans.add(bean)
                    }
                }
            }

            igniteApplicationContext.beanDefinitionNames.each { beanDefName ->
                def bean = igniteApplicationContext.getBean(beanDefName)
                if (bean instanceof CacheConfiguration) {
                    log.info "found manually-configured cache bean ${beanDefName}"
                    cacheConfigurationBeans.add(bean)
                }
            }

            grid.configuration().setCacheConfiguration(cacheConfigurationBeans as CacheConfiguration[])

            log.info "Starting Ignite grid..."
            grid.start()
            grid.services().deployClusterSingleton("distributedSchedulerService", new DistributedSchedulerServiceImpl())

        } catch (NoSuchBeanDefinitionException e) {
            log.warn e.message
            return false
        } catch (IgniteCheckedException e) {
            log.error e.message, e
            return false
        }

//        ctx.getBean('distributedSchedulerService').grid = grid
        return true
    }

    static ApplicationContext getIgniteApplicationContext() {
        return igniteApplicationContext
    }

//    static CacheConfiguration getSpringConfiguredCache(String name) {
//        try {
//            return igniteApplicationContext.getBean(name)
//        } catch (NoSuchBeanDefinitionException e) {
//            return null
//        }
//    }
//
//    static boolean startIgniteProgramatically() {
//        def ctx = Holders.applicationContext
//        def application = Holders.grailsApplication
//
//        def configuredAddresses = []
//        def conf = Holders.grailsApplication.config.ignite
//        if (conf.discoverySpi.containsKey('addresses')) {
//            configuredAddresses = conf.discoverySpi.addresses
//        }
//
//        IgniteConfiguration config = new IgniteConfiguration()
//        config.setMarshaller(new OptimizedMarshaller(false))
//        def discoverySpi = new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi()
//        discoverySpi.setNetworkTimeout(5000)
//        def ipFinder = new org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder()
//        ipFinder.setAddresses(configuredAddresses)
//        discoverySpi.setIpFinder(ipFinder)
//        def grid = Ignition.start(config)
//
//        return grid != null
//    }
//
//    static ApplicationContext getApplicationContext() {
//        return igniteApplicationContext
//    }
}
