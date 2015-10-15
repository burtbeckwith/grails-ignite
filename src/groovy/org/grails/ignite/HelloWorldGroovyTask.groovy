package org.grails.ignite

/**
 * An example Runnable used to demonstrate the DistributedSchedulerService
 */
class HelloWorldGroovyTask implements Serializable, Runnable {
    void run() {
        println("hello from ${getClass().name} at ${new Date()}")
    }
}
