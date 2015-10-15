grails.project.work.dir = 'target'

def igniteVer = '1.3.0-incubating'
def igniteHibernateVer = '1.2.0-incubating'

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    inherits("global") {
        excludes 'h2'
    }
    log "warn"
    repositories {
        mavenLocal()
        grailsCentral()
        mavenCentral()
    }

    dependencies {
        compile "com.h2database:h2:1.3.175"
        compile "org.apache.ignite:ignite-core:${igniteVer}"
        compile ("org.apache.ignite:ignite-spring:${igniteVer}") {
            excludes 'spring-core', 'spring-aop', 'spring-beans', 'spring-context', 'spring-expression', 'spring-tx'
        }
        compile "org.apache.ignite:ignite-indexing:${igniteVer}"
        compile("org.apache.ignite:ignite-hibernate:${igniteHibernateVer}") {
            excludes 'hibernate-core'
        }
        compile "org.apache.ignite:ignite-web:${igniteVer}"
        compile "org.apache.ignite:ignite-log4j:${igniteVer}"
        compile "org.apache.ignite:ignite-rest-http:${igniteVer}"
        compile "org.apache.ignite:ignite-aws:${igniteVer}"

        compile 'org.bouncycastle:bcprov-jdk15on:1.52'
        compile 'com.google.code.findbugs:jsr305:3.0.0'
        compile 'com.cedarsoftware:groovy-io:1.1.1'
        compile 'it.sauronsoftware.cron4j:cron4j:2.2.5'
    }

    plugins {
        build(":release:3.1.1", ":rest-client-builder:2.1.1") {
            export = false
        }

        compile ':webxml:1.4.1'

        runtime ":hibernate4:4.3.8.1"
    }
}
