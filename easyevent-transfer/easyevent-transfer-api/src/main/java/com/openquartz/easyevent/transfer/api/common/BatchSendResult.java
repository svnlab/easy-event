package com.openquartz.easyevent.transfer.api.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量发送结果
 *
 * @author svnee
 **/
public class BatchSendResult {

    /**
     * 发送下标集合
     */
    private final List<Integer> sendCompletedIndexList;

    /**
     * 失败的索引下标
     */
    private final List<Integer> sendFailedIndexList;

    /**
     * failed exception
     */
    private Exception failedException;

    public BatchSendResult() {
        this.sendCompletedIndexList = new ArrayList<>();
        this.sendFailedIndexList = new ArrayList<>();
    }

    public void addCompletedIndex(List<Integer> completedEventIndexList) {
        sendCompletedIndexList.addAll(completedEventIndexList);
    }

    public void addFailedIndex(List<Integer> failedIndexList) {
        sendFailedIndexList.addAll(failedIndexList);
    }

    public void addCompletedIndex(Integer completedEventIndex) {
        sendCompletedIndexList.add(completedEventIndex);
    }

    public void addFailedIndex(Integer failedEventIndex) {
        sendFailedIndexList.add(failedEventIndex);
    }

    public List<Integer> getSendCompletedIndexList() {
        return sendCompletedIndexList;
    }

    public List<Integer> getSendFailedIndexList() {
        return sendFailedIndexList;
    }

    public Exception getFailedException() {
        return failedException;
    }

    public void setFailedException(Exception failedException) {
        this.failedException = failedException;
    }
}
