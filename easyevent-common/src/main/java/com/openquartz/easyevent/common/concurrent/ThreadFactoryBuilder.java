package com.openquartz.easyevent.common.concurrent;

import com.openquartz.easyevent.common.utils.Asserts;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import com.openquartz.easyevent.common.exception.CommonErrorCode;

/**
 * ThreadFactory builder.
 *
 * @author svnee
 */
public final class ThreadFactoryBuilder {

    private String nameFormat = null;
    private Boolean daemon = null;
    private Integer priority = null;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
    private ThreadFactory backingThreadFactory = null;

    public ThreadFactoryBuilder setNameFormat(String nameFormat) {
        format(nameFormat, 0);
        this.nameFormat = nameFormat;
        return this;
    }

    public ThreadFactoryBuilder setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public ThreadFactoryBuilder setPriority(int priority) {
        Asserts.isTrue(priority >= 1, CommonErrorCode.THREAD_PRIORITY_SETTER_MORE_ERROR, priority, 1);
        Asserts.isTrue(priority <= 10, CommonErrorCode.THREAD_PRIORITY_SETTER_LESS_THAN_ERROR, priority, 10);
        this.priority = priority;
        return this;
    }

    public ThreadFactoryBuilder setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        Asserts.notNull(uncaughtExceptionHandler, CommonErrorCode.THREAD_EXECUTE_EXCEPTION_NULLABLE_ERROR);
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    public ThreadFactoryBuilder setThreadFactory(ThreadFactory backingThreadFactory) {
        Asserts.notNull(backingThreadFactory, CommonErrorCode.THREAD_POOL_FACTORY_NULLABLE_ERROR);
        this.backingThreadFactory = backingThreadFactory;
        return this;
    }

    public ThreadFactory build() {
        return doBuild(this);
    }

    private static ThreadFactory doBuild(ThreadFactoryBuilder builder) {
        final String nameFormat = builder.nameFormat;
        final Boolean daemon = builder.daemon;
        final Integer priority = builder.priority;
        final UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
        final ThreadFactory backingThreadFactory =
            builder.backingThreadFactory != null ? builder.backingThreadFactory : Executors.defaultThreadFactory();
        final AtomicLong count = nameFormat != null ? new AtomicLong(0L) : null;
        return runnable -> {
            Thread thread = backingThreadFactory.newThread(runnable);
            if (nameFormat != null) {
                thread.setName(ThreadFactoryBuilder
                    .format(nameFormat, Objects.requireNonNull(count).getAndIncrement()));
            }

            if (daemon != null) {
                thread.setDaemon(daemon);
            }

            if (priority != null) {
                thread.setPriority(priority);
            }

            if (uncaughtExceptionHandler != null) {
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            }

            return thread;
        };
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }


}
