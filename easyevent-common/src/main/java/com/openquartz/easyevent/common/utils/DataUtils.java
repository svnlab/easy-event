package com.openquartz.easyevent.common.utils;

import com.openquartz.easyevent.common.exception.DataErrorCode;

/**
 * DataUtils
 *
 * @author svnee
 **/
public final class DataUtils {

    private DataUtils() {
    }

    public static void checkInsertOne(int actual) {
        checkInsertNums(1, actual);
    }

    public static void checkInsertNums(int affect, int actual) {
        Asserts.isTrue(affect == actual, DataErrorCode.INSERT_ERROR, affect, actual);
    }

    public static void checkUpdateOne(int actual) {
        checkUpdateNums(1, actual);
    }

    public static void checkUpdateNums(int affect, int actual) {
        Asserts.isTrue(affect == actual, DataErrorCode.UPDATE_ERROR, affect, actual);
    }

    public static void checkDeleteOne(int actual) {
        checkDeleteNums(1, actual);
    }

    public static void checkDeleteNums(int affect, int actual) {
        Asserts.isTrue(affect == actual, DataErrorCode.DELETE_ERROR, affect, actual);
    }

}
