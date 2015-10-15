import grails.plugin.webxml.FilterManager
import org.grails.ignite.IgniteContextBridge
import org.grails.ignite.IgniteStartupHelper
import org.grails.ignite.WebSessionFilter

class IgniteGrailsPlugin {
    def version = "0.3.1-SNAPSHOT"
    def grailsVersion = "2.3 > *"
    def title = "Grails Ignite Plugin"
    def author = "Dan Stieglitz"
    def authorEmail = "dstieglitz@stainlesscode.com"
    def description = 'A plugin for the Apache Ignite data grid framework.'
    def documentation = "http://grails.org/plugin/ignite"
    def license = "APACHE"
    def developers = [[name: "Dan Stieglitz", email: "dstieglitz@stainlesscode.com"]]
    def issueManagement = [system: "GITHUB", url: "https://github.com/dstieglitz/grails-ignite/issues"]
    def scm = [url: "https://github.com/dstieglitz/grails-ignite"]
    def loadAfter = ['logging']
    def loadBefore = ['hibernate', 'hibernate4']

    def getWebXmlFilterOrder() {
        [IgniteWebSessionsFilter: FilterManager.CHAR_ENCODING_POSITION + 100]
    }

    def doWithWebDescriptor = { xml ->
        def configuredGridName = IgniteStartupHelper.DEFAULT_GRID_NAME
        def conf = application.config.ignite
        if (conf.containsKey('gridName')) {
            configuredGridName = conf.gridName
        }

        // FIXME no log.(anything) output from here
        //println "Web session clustering enabled in config? ${webSessionClusteringEnabled} for gridName=${configuredGridName}"

//        //
//        // FIXME this will be checked at BUILD time and therefore must be "true" if the filters are to be installed
//        //
//        if (webSessionClusteringEnabled) {
//            def listenerNode = xml.'listener'
//            listenerNode[listenerNode.size() - 1] + {
//                listener {
//                    'listener-class'('org.apache.ignite.startup.servlet.ServletContextListenerStartup')
//                }
//            }

        def contextParam = xml.'context-param'
        contextParam[contextParam.size() - 1] + {
            filter {
                'filter-name'('IgniteWebSessionsFilter')
                'filter-class'(WebSessionFilter.name)
                'init-param' {
                    'param-name'('IgniteWebSessionsGridName')
                    'param-value'(configuredGridName)
                }
            }
        }

        def filterMappingNode = xml.'filter-mapping'
        filterMappingNode[filterMappingNode.size() - 1] + {
            'filter-mapping' {
                'filter-name'('IgniteWebSessionsFilter')
                'url-pattern'('/*')
            }
        }

        contextParam[contextParam.size() - 1] + {
            'context-param' {
                'param-name'('IgniteWebSessionsCacheName')
                'param-value'(IgniteStartupHelper.IGNITE_WEB_SESSION_CACHE_NAME)
            }
        }
//        }
    }

    def doWithSpring = {
        def conf = application.config.ignite
        if ((conf.enabled instanceof Boolean) && conf.enabled) {
            grid(IgniteContextBridge)
        }
    }
}
