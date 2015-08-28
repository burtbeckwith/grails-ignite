import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheWriteSynchronizationMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy
import org.apache.ignite.configuration.CacheConfiguration
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.ignite.IgniteStartupHelper

beans {
    // FIXME externalize
    def l2CacheEnabled = true

    def webSessionClusteringEnabled = (!(application.config.ignite.webSessionClusteringEnabled instanceof ConfigObject)
            && application.config.ignite.webSessionClusteringEnabled.equals(true))

    if (webSessionClusteringEnabled) {
        webSessionClusterCacheConfigurationBean(CacheConfiguration) {
            name = IgniteStartupHelper.IGNITE_WEB_SESSION_CACHE_NAME
            cacheMode = CacheMode.PARTITIONED
            backups = 1
            evictionPolicy = { LruEvictionPolicy lruEvictionPolicy ->
                maxSize = 10000
            }
        }
    }

    if (l2CacheEnabled) {
        // Hibernate L2 cache parent configurations
        atomicCache(CacheConfiguration) { bean ->
            name = 'atomicCache'
            cacheMode = CacheMode.PARTITIONED
            atomicityMode = CacheAtomicityMode.ATOMIC
            writeSynchronizationMode = CacheWriteSynchronizationMode.FULL_SYNC
        }

        transactionalCache(CacheConfiguration) { bean ->
            name = 'transactionalCache'
            cacheMode = CacheMode.PARTITIONED
            atomicityMode = CacheAtomicityMode.TRANSACTIONAL
            writeSynchronizationMode = CacheWriteSynchronizationMode.FULL_SYNC
        }

        // FIXME allow external cache configuration for optimization on a class-by-class basis
        // Hibernate L2 cache parent configurations
        application.domainClasses.each { clazz ->
            def binder = new GrailsDomainBinder()
            def mapping = binder.getMapping(clazz)
            if (mapping.cache && mapping.cache.enabled) {
                "domainClassCache_${clazz.fullName}" { bean ->
                    if (mapping.cache.usage.equalsIgnoreCase("read-write")) {
                        bean.parent = ref('transactionalCache')
                    } else {
                        bean.parent = ref('atomicCache')
                    }

                    name = "${clazz.fullName}"
                }

                // FIXME now do associations
                clazz.associationMap.each { k, v ->
                    if (colMapping.cache && colMapping.cache.enabled) {

                    }
                    println "${k}=${v}"

                }
            }
        }

        'org.hibernate.cache.spi.UpdateTimestampsCache' { bean ->
            bean.parent = ref('atomicCache')
            name = 'org.hibernate.cache.spi.UpdateTimestampsCache'
        }

        // Hibernate query cache
        'org.hibernate.cache.internal.StandardQueryCache' { bean ->
            bean.parent = ref('atomicCache')
            name = 'org.hibernate.cache.internal.StandardQueryCache'
        }
    }
}