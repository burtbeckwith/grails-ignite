import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheWriteSynchronizationMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.events.EventType
import org.apache.ignite.marshaller.optimized.OptimizedMarshaller
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import org.grails.ignite.DeferredStartIgniteSpringBean
import org.grails.ignite.IgniteGrailsLogger

//import org.apache.ignite.logger.log4j.Log4JLogger
import org.grails.ignite.IgniteStartupHelper

beans {
    def conf = application.config.ignite

    def peerClassLoadingEnabledInConfig = (conf.peerClassLoadingEnabled instanceof Boolean) &&
            conf.peerClassLoadingEnabled

    def configuredGridName = IgniteStartupHelper.DEFAULT_GRID_NAME
    if (conf.containsKey('gridName')) {
        configuredGridName = conf.gridName
    }

    def configuredNetworkTimeout = 3000
    if (conf.discoverySpi.containsKey('networkTimeout')) {
        configuredNetworkTimeout = conf.discoverySpi.networkTimeout
    }

    def configuredAddresses = []
    if (conf.discoverySpi.containsKey('addresses')) {
        configuredAddresses = conf.discoverySpi.addresses
    }

    def igniteEnabled = (conf.enabled instanceof Boolean) && conf.enabled

    /*
     * Only configure Ignite if the configuration value ignite.enabled=true is defined
     */
    if (igniteEnabled) {
        // FIXME https://github.com/dstieglitz/grails-ignite/issues/1
        gridLogger(IgniteGrailsLogger)

        igniteCfg(IgniteConfiguration) {
            gridName = configuredGridName
            peerClassLoadingEnabled = peerClassLoadingEnabledInConfig

            marshaller = { OptimizedMarshaller marshaller ->
                requireSerializable = false
            }

            //            marshaller = { JdkMarshaller marshaller ->
            ////                requireSerializable = false
            //            }

            //            marshaller = { GroovyOptimizedMarshallerDecorator dec ->
            //                underlyingMarshaller = { OptimizedMarshaller mar ->
            //                    requireSerializable = false
            //                }
            //            }

            includeEventTypes = [
                    EventType.EVT_TASK_STARTED,
                    EventType.EVT_TASK_FINISHED,
                    EventType.EVT_TASK_FAILED,
                    EventType.EVT_TASK_TIMEDOUT,
                    EventType.EVT_TASK_SESSION_ATTR_SET,
                    EventType.EVT_TASK_REDUCED,
                    EventType.EVT_CACHE_OBJECT_PUT,
                    EventType.EVT_CACHE_OBJECT_READ]

            discoverySpi = { TcpDiscoverySpi discoverySpi ->
                networkTimeout = configuredNetworkTimeout
                ipFinder = { TcpDiscoveryMulticastIpFinder tcpDiscoveryMulticastIpFinder ->
                    addresses = configuredAddresses
                }
            }

//                deploymentSpi = { LowcalDeploymentSpi impl ->
//
//                }

//                serviceConfiguration = [{ ServiceConfiguration serviceConfiguration ->
//                    name = "distributedSchedulerService"
//                    maxPerNodeCount = 1
//                    totalCount = 1
//                    service = { DistributedSchedulerServiceImpl impl -> }
//                }]

            gridLogger = ref('gridLogger')
        }

        grid(DeferredStartIgniteSpringBean) { bean ->
            bean.lazyInit = true
//            bean.dependsOn = ['persistenceInterceptor']
            configuration = ref('igniteCfg')
        }
    }
}
