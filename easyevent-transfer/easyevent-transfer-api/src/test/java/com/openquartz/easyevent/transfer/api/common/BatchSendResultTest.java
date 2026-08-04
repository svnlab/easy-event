package com.openquartz.easyevent.transfer.api.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

/**
 * Regression test for {@link BatchSendResult}: failed indexes must be
 * collected in {@code sendFailedIndexList}, not in {@code sendCompletedIndexList}.
 * Previously a copy-paste error wrote failed indexes into the completed list,
 * causing batch-sent events that actually failed to be marked as
 * TRANSFER_SUCCESS (and silently lost, since that state is not compensated).
 *
 * @author easy-event
 */
public class BatchSendResultTest {

    @Test
    public void testAddFailedIndexAppendsToFailedList() {
        BatchSendResult result = new BatchSendResult();
        result.addFailedIndex(2);
        result.addFailedIndex(5);

        assertEquals(Arrays.asList(2, 5), result.getSendFailedIndexList());
        assertTrue(result.getSendCompletedIndexList().isEmpty());
    }

    @Test
    public void testAddFailedIndexListAppendsToFailedList() {
        BatchSendResult result = new BatchSendResult();
        result.addFailedIndex(Arrays.asList(0, 1, 3));

        assertEquals(Arrays.asList(0, 1, 3), result.getSendFailedIndexList());
        assertTrue(result.getSendCompletedIndexList().isEmpty());
    }

    @Test
    public void testCompletedAndFailedIndexesStaySeparated() {
        BatchSendResult result = new BatchSendResult();
        result.addCompletedIndex(1);
        result.addCompletedIndex(4);
        result.addFailedIndex(2);

        assertEquals(Arrays.asList(1, 4), result.getSendCompletedIndexList());
        assertEquals(Arrays.asList(2), result.getSendFailedIndexList());
    }
}
