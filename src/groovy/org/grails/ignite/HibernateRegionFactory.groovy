package org.grails.ignite

import grails.util.Holders
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMemoryMode
import org.apache.ignite.cache.CacheWriteSynchronizationMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy
import org.apache.ignite.cache.hibernate.HibernateRegionFactory as IgniteHibernateRegionFactory
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.hibernate.cache.CacheException
import org.hibernate.cache.spi.*
import org.hibernate.cache.spi.access.AccessType
import org.hibernate.cfg.Settings

/**
 * @author dstieglitz
 */
class HibernateRegionFactory implements RegionFactory {

    private static final String ASSOCIATION_CACHE_MEMORY_MODE_KEY = 'ignite.l2cache.associationMemoryMode'
    private static final String ASSOCIATION_CACHE_ATOMICITY_MODE_KEY = 'ignite.l2cache.associationAtomicityMode'
    private static final String ASSOCIATION_CACHE_WRITE_SYNC_MODE_KEY = 'ignite.l2cache.associationWriteSynchronizationMode'
    private static final String ASSOCIATION_CACHE_MAX_SIZE = 'ignite.l2cache.associationMaxSize'
    private static final String ASSOCIATION_CACHE_EVICT_SYNCHRONIZED = 'ignite.l2cache.associationEvictSynchronized'
    private static final String ENTITY_CACHE_MEMORY_MODE_KEY = 'ignite.l2cache.entityMemoryMode'
    private static final String ENTITY_CACHE_ATOMICITY_MODE_KEY = 'ignite.l2cache.entityAtomicityMode'
    private static final String ENTITY_CACHE_WRITE_SYNC_MODE_KEY = 'ignite.l2cache.entityWriteSynchronizationMode'
    private static final String ENTITY_CACHE_MAX_SIZE = 'ignite.l2cache.entityMaxSize'
    private static final String ENTITY_CACHE_EVICT_SYNCHRONIZED = 'ignite.l2cache.entityEvictSynchronized'

    private static final Logger log = Logger.getLogger(this)
    private IgniteHibernateRegionFactory underlyingRegionFactory = new IgniteHibernateRegionFactory()
    private boolean igniteNodeInitialized

    private boolean init() {
        log.debug "init() --> igniteNodeInitialized=${igniteNodeInitialized}"
        if (igniteNodeInitialized) {
            return
        }

        if (IgniteStartupHelper.startIgnite()) {
            igniteNodeInitialized = true
            return true
        }

        return false
    }

    @Override
    void start(Settings settings, Properties properties) throws CacheException {
        log.debug("Ignite HibernateRegionFactory start() with settings=${settings}, properties=${properties}")

        //
        // we need to re-write property names here, Grails will prepend "hibernate." to them
        //
        def gridName = properties.getProperty("hibernate.${IgniteHibernateRegionFactory.GRID_NAME_PROPERTY}")
        log.debug "grid name is ${gridName}"
        if (gridName) {
            properties.setProperty(IgniteHibernateRegionFactory.GRID_NAME_PROPERTY, gridName)
        }

        if (init()) {
            log.debug "starting underlyingRegionFactory"
            underlyingRegionFactory.start(settings, properties)
        }
    }

    @Override
    void stop() {
        log.debug("Ignite HibernateRegionFactory stop()")
        underlyingRegionFactory.stop()
    }

    @Override
    boolean isMinimalPutsEnabledByDefault() {
        return underlyingRegionFactory.isMinimalPutsEnabledByDefault()
    }

    @Override
    AccessType getDefaultAccessType() {
        return underlyingRegionFactory.getDefaultAccessType()
    }

    @Override
    long nextTimestamp() {
        return underlyingRegionFactory.nextTimestamp()
    }

    @Override
    EntityRegion buildEntityRegion(String s, Properties properties, CacheDataDescription cacheDataDescription) throws CacheException {
        configureEntityCache(s)
        return underlyingRegionFactory.buildEntityRegion(s, properties, cacheDataDescription)
    }

    @Override
    NaturalIdRegion buildNaturalIdRegion(String s, Properties properties, CacheDataDescription cacheDataDescription) throws CacheException {
        return underlyingRegionFactory.buildNaturalIdRegion(s, properties, cacheDataDescription)
    }

    @Override
    CollectionRegion buildCollectionRegion(String s, Properties properties, CacheDataDescription cacheDataDescription) throws CacheException {
        // check if the cache exists, create it if not
        log.debug "buildCollectionRegion(${s}, ${properties}, ${cacheDataDescription})"
        configureAssociationCache(s)

        return underlyingRegionFactory.buildCollectionRegion(s, properties, cacheDataDescription)
    }

    @Override
    QueryResultsRegion buildQueryResultsRegion(String s, Properties properties) throws CacheException {
        return underlyingRegionFactory.buildQueryResultsRegion(s, properties)
    }

    @Override
    TimestampsRegion buildTimestampsRegion(String s, Properties properties) throws CacheException {
        return underlyingRegionFactory.buildTimestampsRegion(s, properties)
    }

    private void configureEntityCache(String entityName) {
        int configuredCaches = IgniteStartupHelper.grid.configuration().getCacheConfiguration().findAll {
            it.name == entityName
        }.size()

        if (configuredCaches == 0) {
//            def springConfiguration = IgniteStartupHelper.getSpringConfiguredCache(entityName)
//            if (springConfiguration != null) {
//                log.info "found a manually-configured cache for ${entityName}, will configure from external configuration"
//                IgniteStartupHelper.grid.addCacheConfiguration(springConfiguration)
//            } else {
            def grailsDomainClass = Holders.grailsApplication.getDomainClass(entityName)
            log.debug "interrogating grails domain class ${entityName} for cache information"
            log.debug "creating default cache for ${entityName}"

            CacheConfiguration cc = new CacheConfiguration(entityName)
            def binder = new GrailsDomainBinder()
            def mapping = binder.getMapping(grailsDomainClass)
            log.debug "found mapping ${mapping} for ${grailsDomainClass}"

            def atomicityMode = valueOrDefault(ENTITY_CACHE_ATOMICITY_MODE_KEY, CacheAtomicityMode.TRANSACTIONAL)
            log.debug "setting atomicity mode for ${entityName} cache to ${atomicityMode}"
            cc.setAtomicityMode(atomicityMode)

            def memoryMode = valueOrDefault(ENTITY_CACHE_MEMORY_MODE_KEY, CacheMemoryMode.OFFHEAP_TIERED)
            log.debug "setting memory mode for ${entityName} cache to ${memoryMode}"
            cc.setMemoryMode(memoryMode)

            def syncMode = valueOrDefault(ENTITY_CACHE_WRITE_SYNC_MODE_KEY, CacheWriteSynchronizationMode.FULL_SYNC)
            log.debug "setting sync mode for ${entityName} cache to ${syncMode}"
            cc.setWriteSynchronizationMode(syncMode)

            cc.setEvictSynchronized(valueOrDefault(ENTITY_CACHE_EVICT_SYNCHRONIZED, false))

            // @see http://apacheignite.gridgain.org/docs/performance-tips
            cc.setBackups(0)
            cc.setOffHeapMaxMemory(0)
            LruEvictionPolicy evictionPolicy = new LruEvictionPolicy()
            evictionPolicy.setMaxSize(valueOrDefault(ENTITY_CACHE_MAX_SIZE, 1000000))
            cc.setEvictionPolicy(evictionPolicy)
            cc.setSwapEnabled(false)

//            if (mapping?.cache?.usage?.equalsIgnoreCase("read-write")) {
//
//            }

            IgniteStartupHelper.grid.getOrCreateCache(cc)
//            }
        }
    }

    private void configureAssociationCache(String associationName) {
        int configuredCaches = IgniteStartupHelper.grid.configuration().getCacheConfiguration().findAll {
            it.name.equals(associationName)
        }.size()

        if (configuredCaches == 0) {
//            def springConfiguration = IgniteStartupHelper.getSpringConfiguredCache(associationName)
//            if (springConfiguration != null) {
//                log.info "found a manually-configured cache for ${associationName}, will configure from external configuration"
//                IgniteStartupHelper.grid.addCacheConfiguration(springConfiguration)
//            } else {
            def grailsDomainClassName = associationName.substring(0, associationName.lastIndexOf('.'))
            def grailsDomainClass = Holders.grailsApplication.getDomainClass(grailsDomainClassName)
            log.debug "interrogating grails domain class ${grailsDomainClassName} for cache information"
            log.debug "creating default cache for ${associationName}"

            CacheConfiguration cc = new CacheConfiguration(associationName)
            def binder = new GrailsDomainBinder()
            def mapping = binder.getMapping(grailsDomainClass)
            log.debug "found mapping ${mapping} for ${grailsDomainClass}"

            def atomicityMode = valueOrDefault(ASSOCIATION_CACHE_ATOMICITY_MODE_KEY, CacheAtomicityMode.TRANSACTIONAL)
            log.debug "setting atomicity mode for ${associationName} cache to ${atomicityMode}"
            cc.setAtomicityMode(atomicityMode)

            def memoryMode = valueOrDefault(ASSOCIATION_CACHE_MEMORY_MODE_KEY, CacheMemoryMode.OFFHEAP_TIERED)
            log.debug "setting memory mode for ${associationName} cache to ${memoryMode}"
            cc.setMemoryMode(memoryMode)

            def syncMode = valueOrDefault(ASSOCIATION_CACHE_WRITE_SYNC_MODE_KEY, CacheWriteSynchronizationMode.FULL_SYNC)
            log.debug "setting sync mode for ${associationName} cache to ${syncMode}"
            cc.setWriteSynchronizationMode(syncMode)

            cc.setEvictSynchronized(valueOrDefault(ASSOCIATION_CACHE_EVICT_SYNCHRONIZED, false))

            // @see http://apacheignite.gridgain.org/docs/performance-tips
            cc.setBackups(0)
            cc.setOffHeapMaxMemory(0)
            LruEvictionPolicy evictionPolicy = new LruEvictionPolicy()
            evictionPolicy.setMaxSize(valueOrDefault(ASSOCIATION_CACHE_MAX_SIZE, 1000000))
            cc.setEvictionPolicy(evictionPolicy)
            cc.setSwapEnabled(false)

//            if (mapping?.cache?.usage?.equalsIgnoreCase("read-write")) {
//
//            }

            IgniteStartupHelper.grid.getOrCreateCache(cc)
//            }
        }
    }

    def valueOrDefault(configName, defaultValue = null) {
        log.debug "valueOrDefault(${configName}, ${defaultValue})"
        def value = Holders.flatConfig.containsKey(configName) ? Holders.flatConfig[configName] : null
        log.debug "got value=${value}"

        if (value == null) {
            value = defaultValue
        }

        return value
    }
}
