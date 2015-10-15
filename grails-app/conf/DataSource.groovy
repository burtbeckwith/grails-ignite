dataSource {
    pooled = true
    jmxExport = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = "update"
    url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true

    cache.region.factory_class = 'org.grails.ignite.HibernateRegionFactory'
    org.apache.ignite.hibernate.grid_name = 'grid'
    org.apache.ignite.hibernate.default_access_type = 'READ_ONLY' // see Ignite docs
}
