package org.grails.ignite

/**
 * <b>Work-in-progress</b>
 * <p>A wrapper class designed to allow the submission of a closure to the Ignite task grid</p>
 */
class IgniteClosureJobWrapper implements Runnable, Serializable {

    private Closure closure
    private result

    IgniteClosureJobWrapper(Closure closure) {
        this.closure = closure
    }

    void run() {
        result = closure.call()
    }

    def get() {
        return result
    }
}
