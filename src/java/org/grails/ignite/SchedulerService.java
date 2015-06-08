package org.grails.ignite;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Interface for a distributed scheduler service.
 *
 * @author Dan Stieglitz
 */
public interface SchedulerService {

    public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

    public void stopScheduler();

    public void startScheduler();

    public boolean isSchedulerRunning();

    public boolean isScheduled(String id);

}
