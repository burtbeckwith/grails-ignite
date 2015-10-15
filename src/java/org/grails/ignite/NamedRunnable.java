package org.grails.ignite;

import org.apache.ignite.lang.IgniteRunnable;

/**
 * Implementations introduce a name to identify Runnables for a scheduling system.
 */
public interface NamedRunnable extends IgniteRunnable {

    String getName();
}
