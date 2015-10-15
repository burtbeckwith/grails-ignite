import org.grails.ignite.IgniteStartupHelper

class IgniteBootStrap {
    def grailsApplication

    def init = { servletContext ->
        def conf = grailsApplication.config.ignite
        def webSessionClusteringEnabled = (conf.webSessionClusteringEnabled instanceof Boolean) &&
                conf.webSessionClusteringEnabled

        log.info "webSessionClustering enabled in config? ${webSessionClusteringEnabled}"
    }
}
