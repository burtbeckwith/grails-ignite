ignite.enabled = true
ignite.config.locations = ['ignite/conf/*.groovy']
ignite.l2CacheEnabled = true

log4j = {
    trace 'groovy.lang.GroovyClassLoader'
    debug 'grails.app.services'

    error  'org.codehaus.groovy.grails',
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    info 'org.apache.ignite'
    debug 'org.grails.ignite'
}
