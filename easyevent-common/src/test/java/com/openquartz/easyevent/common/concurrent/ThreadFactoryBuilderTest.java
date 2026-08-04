package com.openquartz.easyevent.common.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.openquartz.easyevent.common.exception.CommonErrorCode;
import com.openquartz.easyevent.common.exception.EasyEventException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * Regression test for {@link ThreadFactoryBuilder#setThreadFactory}:
 * it must validate its own argument (backingThreadFactory), not the
 * unrelated uncaughtExceptionHandler field.
 *
 * @author easy-event
 */
public class ThreadFactoryBuilderTest {

    @Test
    public void testSetThreadFactoryAcceptsNonNullFactory() {
        AtomicReference<Thread> created = new AtomicReference<>();
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setThreadFactory(runnable -> {
                    Thread thread = new Thread(runnable, "custom");
                    created.set(thread);
                    return thread;
                })
                .build();

        Thread thread = factory.newThread(() -> {
        });
        assertNotNull(thread);
        assertEquals("custom", thread.getName());
        assertEquals(thread, created.get());
    }

    @Test
    public void testSetThreadFactoryRejectsNullFactory() {
        try {
            new ThreadFactoryBuilder().setThreadFactory(null);
            fail("Expected EasyEventException");
        } catch (EasyEventException ex) {
            assertEquals(CommonErrorCode.THREAD_POOL_FACTORY_NULLABLE_ERROR, ex.getErrorCode());
        }
    }

    @Test
    public void testDefaultBackingFactoryUsedWhenNotSet() {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("default-%d").build();
        assertNotNull(factory.newThread(() -> {
        }));
    }

    @Test
    public void testExecutorsDefaultFactoryAccepted() {
        // Regression: a valid factory was previously rejected when the
        // uncaughtExceptionHandler had not been set.
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setThreadFactory(Executors.defaultThreadFactory())
                .build();
        assertNotNull(factory.newThread(() -> {
        }));
    }
}
