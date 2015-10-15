package org.grails.ignite;

import java.util.concurrent.ScheduledFuture;

/**
 * A distributed scheduler service. The service can schedule objects of type NamedRunnable, which
 * provide a getName() method that returns an identifier for the Runnable. The identifier is used to lookup scheduled
 * tasks for cancellation or retrieving specific results (or exceptions).
 *
 * @author Dan Stieglitz
 */
@SuppressWarnings("rawtypes")
public interface SchedulerService {

    ScheduledFuture scheduleAtFixedRate(ScheduledRunnable command);

    ScheduledFuture scheduleWithFixedDelay(ScheduledRunnable command);

    ScheduledFuture scheduleWithCron(ScheduledRunnable command);

    ScheduledFuture schedule(ScheduledRunnable command);

    void stopScheduler();

    void startScheduler();

    boolean isSchedulerRunning();

    boolean isScheduled(String id);

    /**
     * Cancel the task with the specified ID. Returns true if the task was found and the cancel was successful.
     */
    boolean cancel(String name, boolean mayInterruptIfRunning);
}
