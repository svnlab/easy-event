package com.openquartz.easyevent.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.openquartz.easyevent.common.exception.DataErrorCode;
import com.openquartz.easyevent.common.exception.EasyEventException;
import org.junit.Test;

/**
 * Regression test for {@link DataUtils}: checkUpdateOne/checkDeleteOne must
 * report UPDATE_ERROR/DELETE_ERROR respectively, not INSERT_ERROR.
 *
 * @author easy-event
 */
public class DataUtilsTest {

    @Test
    public void testCheckUpdateOnePassesWhenOneRowUpdated() {
        DataUtils.checkUpdateOne(1);
    }

    @Test
    public void testCheckUpdateOneFailsWithUpdateErrorCode() {
        try {
            DataUtils.checkUpdateOne(0);
            fail("Expected EasyEventException");
        } catch (EasyEventException ex) {
            assertEquals(DataErrorCode.UPDATE_ERROR, ex.getErrorCode());
        }
    }

    @Test
    public void testCheckDeleteOneFailsWithDeleteErrorCode() {
        try {
            DataUtils.checkDeleteOne(0);
            fail("Expected EasyEventException");
        } catch (EasyEventException ex) {
            assertEquals(DataErrorCode.DELETE_ERROR, ex.getErrorCode());
        }
    }

    @Test
    public void testCheckInsertOneStillFailsWithInsertErrorCode() {
        try {
            DataUtils.checkInsertOne(0);
            fail("Expected EasyEventException");
        } catch (EasyEventException ex) {
            assertEquals(DataErrorCode.INSERT_ERROR, ex.getErrorCode());
        }
    }
}
