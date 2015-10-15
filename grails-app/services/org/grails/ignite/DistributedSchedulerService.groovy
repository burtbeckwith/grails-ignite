package org.grails.ignite

import org.apache.ignite.compute.ComputeExecutionRejectedException
import org.apache.ignite.compute.ComputeTaskFuture
import org.apache.ignite.lang.IgniteUuid
import org.grails.ignite.ScheduledRunnable
import org.grails.ignite.SchedulerService

import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class DistributedSchedulerService {

    static transactional = false

    def grid

    ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit, String name = null) {
        log.debug "scheduleAtFixedRate ${command}, ${initialDelay}, ${period}, ${unit}"

        ScheduledRunnable scheduledRunnable = name ? new ScheduledRunnable(name, command) : new ScheduledRunnable(command)

        if (serviceProxy.isScheduled(scheduledRunnable.name)) {
            throw new ComputeExecutionRejectedException("Won't schedule underlyingRunnable that's already scheduled: $scheduledRunnable.name")
        }

        scheduledRunnable.initialDelay = initialDelay
        scheduledRunnable.period = period
        scheduledRunnable.timeUnit = unit

        def future = serviceProxy.scheduleAtFixedRate(scheduledRunnable)
        log.debug "getServiceProxy().scheduleAtFixedRate returned future ${future}"
        return future
    }

    ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit, String name = null) {
        log.debug "scheduleWithFixedDelay ${command}, ${initialDelay}, ${delay}, ${unit}"

        ScheduledRunnable scheduledRunnable = name ? new ScheduledRunnable(name, command) : new ScheduledRunnable(command)
        scheduledRunnable.initialDelay = initialDelay
        scheduledRunnable.delay = delay
        scheduledRunnable.timeUnit = unit

        if (serviceProxy.isScheduled(scheduledRunnable)) {
            throw new ComputeExecutionRejectedException("Won't schedule underlyingRunnable that's already scheduled: $scheduledRunnable.name")
        }

        def future = serviceProxy.scheduleWithFixedDelay(scheduledRunnable)
        log.debug "getServiceProxy().scheduleWithFixedDelay returned future ${future}"
        return future
    }

    ScheduledFuture schedule(Runnable command, long delay, TimeUnit unit, String name = null) {
        log.debug "scheduleWithFixedDelay ${command}, ${delay}, ${unit}"

        ScheduledRunnable scheduledRunnable = name ? new ScheduledRunnable(name, command) : new ScheduledRunnable(command)
        scheduledRunnable.initialDelay = delay
        scheduledRunnable.timeUnit = unit

        if (serviceProxy.isScheduled(scheduledRunnable)) {
            throw new ComputeExecutionRejectedException("Won't schedule underlyingRunnable that's already scheduled: $scheduledRunnable.name")
        }

        def future = serviceProxy.schedule(scheduledRunnable)
        log.debug "getServiceProxy().schedule returned future ${future}"
        return future
    }

    ScheduledFuture scheduleWithCron(Runnable command, String cronExpression, String name = null) throws Exception {
        log.debug "scheduleWithFixedDelay ${command}, ${cronExpression}"

        ScheduledRunnable scheduledRunnable = name ? new ScheduledRunnable(name, command) : new ScheduledRunnable(command)
        scheduledRunnable.cronString = cronExpression

        if (name != null && serviceProxy.isScheduled(name)) {
            throw new ComputeExecutionRejectedException("Won't schedule underlyingRunnable that's already scheduled: $scheduledRunnable.name")
        }

        def future = serviceProxy.scheduleWithCron(scheduledRunnable)
        log.debug "getServiceProxy().schedule returned future ${future}"
        return future
    }

    Map<IgniteUuid, ComputeTaskFuture> getFutures() {
        return grid.compute().activeTaskFutures()
    }

    boolean cancel(String name, boolean interrupt) {
        log.debug "cancel '${name}', ${interrupt}"
        return serviceProxy.cancel(name, interrupt)
    }

    void stopScheduler() {
        serviceProxy.stopScheduler()
    }

    void startScheduler() {
        serviceProxy.startScheduler()
    }

    boolean isSchedulerRunning() {
        return serviceProxy.schedulerRunning
    }

    boolean isScheduled(String id) {
        return serviceProxy.isScheduled(id)
    }

    private SchedulerService getServiceProxy() {
        return grid.services().serviceProxy("distributedSchedulerService", SchedulerService, false)
    }
}
