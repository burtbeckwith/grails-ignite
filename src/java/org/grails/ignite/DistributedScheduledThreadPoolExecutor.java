package org.grails.ignite;

import it.sauronsoftware.cron4j.Scheduler;
import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.log4j.Logger;

import java.util.concurrent.*;

/**
 * <p>Inspired by the blog post at http://code.nomad-labs.com/2011/12/09/mother-fk-the-scheduledexecutorservice/</p>
 * <p>This class wraps the Java concurrent ScheduledThreadPoolExecutor and submits the underlying runnable to an
 * Ignite compute grid instead of using a local thread pool.</p>
 * <p>This class extends the default ScheduledThreadPoolExecutor interface with CRON functionality</p>
 *
 * @author dstieglitz
 * @author srasul
 * @see http://code.nomad-labs.com/2011/12/09/mother-fk-the-scheduledexecutorservice/
 */
public class DistributedScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger log = Logger.getLogger(DistributedScheduledThreadPoolExecutor.class.getName());
    @IgniteInstanceResource
    private Ignite ignite;
    private boolean running = true;
    private Scheduler cronScheduler;

    public DistributedScheduledThreadPoolExecutor() {
        this(5);
    }

    public DistributedScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
        cronScheduler = new Scheduler();
        cronScheduler.start();
    }

    public DistributedScheduledThreadPoolExecutor(Ignite ignite, int corePoolSize) {
        this(corePoolSize);
        this.ignite = ignite;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(new IgniteDistributedRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(new IgniteDistributedRunnable(command), initialDelay, delay, unit);
    }

    @SuppressWarnings("rawtypes")
    public ScheduledFuture<?> scheduleWithCron(Runnable command, String cronString) {
        IgniteCronDistributedRunnable scheduledFuture = new IgniteCronDistributedRunnable(command);
        String id = cronScheduler.schedule(cronString, scheduledFuture);
        scheduledFuture.setCronTaskId(id);

        // return ScheduledFuture for cron task with embedded id
        return scheduledFuture;
    }

    @SuppressWarnings("rawtypes")
    public boolean cancel(Runnable runnable, boolean mayInterruptIfRunning) {
        if (log.isDebugEnabled()) {
            log.debug("cancel " + runnable + "," + mayInterruptIfRunning);
        }

        if (mayInterruptIfRunning) {
            // FIXME interrupt the Runnable?
            log.warn("mayInterruptIfRunning is currently ignored");
        }

        if (runnable instanceof IgniteCronDistributedRunnable) {
            ((IgniteCronDistributedRunnable) runnable).cancel(mayInterruptIfRunning);
            return true;
        }

        // these are ScheduledFutureTasks
        if (log.isDebugEnabled()) {
            for (Runnable r : getQueue()) {
                log.debug("found queued runnable: " + r);
            }
        }

        return super.remove(runnable);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean trueOrFalse) {
        running = trueOrFalse;
    }

    private class IgniteDistributedRunnable implements IgniteRunnable {
        private static final long serialVersionUID = 1;

        protected Runnable runnable;

        public IgniteDistributedRunnable(Runnable scheduledRunnable) {
            runnable = scheduledRunnable;
        }

        @Override
        public void run() {
//            try {
                if (running) {
                    ignite.executorService().submit(runnable);
                }
//            } catch (Exception e) {
//                // LOG IT HERE!!!
//                log.error("error in executing: " + runnable + ". It will no longer be run!", e);
//
//                // and re throw it so that the Executor also gets this error so that it can do what it would usually do
//                throw new RuntimeException(e);
//            }
        }
    }

    private class IgniteCronDistributedRunnable<V>
            extends IgniteDistributedRunnable
            implements RunnableScheduledFuture<V> {

        private static final long serialVersionUID = 1;

        private String cronTaskId;
        private boolean cancelled;

        public IgniteCronDistributedRunnable(Runnable runnable) {
            super(runnable);
        }

        @Override
        public boolean isPeriodic() {
            throw new UnsupportedOperationException("Operation not supported (yet)");
        }

        @Override
        public long getDelay(TimeUnit unit) {
            throw new UnsupportedOperationException("Operation not supported (yet)");
        }

        @Override
        public int compareTo(Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (cronTaskId == null) {
                throw new IllegalArgumentException("Can't cancel a task without a cron task ID");
            }
            cronScheduler.deschedule(cronTaskId);
            cancelled = true;
            return isCancelled();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException("Operation not supported (yet)");
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException("Operation not supported (yet)");
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException("Operation not supported (yet)");
        }

        public String getCronTaskId() {
            return cronTaskId;
        }

        public void setCronTaskId(String cronTaskId) {
            this.cronTaskId = cronTaskId;
        }
    }
}
