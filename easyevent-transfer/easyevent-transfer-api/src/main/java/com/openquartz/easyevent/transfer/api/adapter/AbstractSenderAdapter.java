package com.openquartz.easyevent.transfer.api.adapter;

import com.openquartz.easyevent.storage.model.EventBody;
import com.openquartz.easyevent.storage.model.EventContext;
import com.openquartz.easyevent.transfer.api.limiting.EventTransferSenderLimitingControl;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyevent.common.transaction.TransactionSupport;
import com.openquartz.easyevent.common.utils.CollectionUtils;
import com.openquartz.easyevent.storage.api.EventStorageService;
import com.openquartz.easyevent.storage.identify.EventId;
import com.openquartz.easyevent.storage.model.BaseEventEntity;
import com.openquartz.easyevent.transfer.api.EventSender;
import com.openquartz.easyevent.transfer.api.common.BatchSendResult;

/**
 * Sender Adapter
 *
 * @author svnee
 **/
@Slf4j
public abstract class AbstractSenderAdapter implements EventSender {

    /**
     * Transfer Producer
     *
     * @return TransferProducer
     */
    public abstract TransferProducer getTransferProducer();

    /**
     * Transaction Support
     *
     * @return TransactionSupport
     */
    public abstract TransactionSupport getTransactionSupport();

    /**
     * Event Storage Service
     *
     * @return EventStorageService
     */
    public abstract EventStorageService getEventStorageService();

    /**
     * EventTransferTriggerLimitingControl
     *
     * @return EventTransferSenderLimitingControl
     */
    public abstract EventTransferSenderLimitingControl getEventTransferSenderLimitingControl();

    /**
     * AsyncSendExecutor
     *
     * @return AsyncSendExecutor
     */
    public abstract Executor getAsyncSendExecutor();

    /**
     * 是否开启异步发送
     *
     * @return 是否开启异步发送
     */
    public boolean isEnableAsyncSend() {
        return false;
    }

    @Override
    public <T> boolean send(T event) {

        EventBody<T> eventBody = new EventBody<>(event, EventContext.get());

        EventId eventId = getTransactionSupport().execute(() -> getEventStorageService().save(eventBody));

        // 事务后触发发送
        getTransactionSupport().executeAfterCommit(() -> getAsyncSendExecutor().execute(() ->
                // 发送限流控制
                getEventTransferSenderLimitingControl().control(event, eventId, (e, eId) -> {

                    if (isEnableAsyncSend()) {
                        doAsyncSendMessage(eId, eventBody);
                    } else {
                        doSyncSendMessage(eId, eventBody);
                    }
                })));
        return true;
    }

    private <T> void doAsyncSendMessage(EventId eId, EventBody<T> eventBody) {

        getTransferProducer().asyncSendMessage(eventBody, eId, new SendResultCallback() {
            @Override
            public void onSuccess(EventId eventId) {
                getTransactionSupport().execute(() -> {
                    getEventStorageService().sendComplete(eId);
                    return true;
                });
            }

            @Override
            public void onFail(EventId eventId, Throwable error) {
                // 存储执行
                getTransactionSupport().execute(() -> {
                    getEventStorageService().sendFailed(eId, error);
                    return true;
                });
            }
        });
    }

    private <T> void doSyncSendMessage(EventId eId, EventBody<T> eventBody) {

        // 发送消息
        try {
            getTransferProducer().sendMessage(eventBody, eId);
        } catch (Exception ex) {
            // 存储执行
            getTransactionSupport().execute(() -> {
                getEventStorageService().sendFailed(eId, ex);
                return true;
            });
            // 发送失败，不能继续标记发送成功
            return;
        }
        // 存储执行
        getTransactionSupport().execute(() -> {
            getEventStorageService().sendComplete(eId);
            return true;
        });
    }

    @Override
    public <T> boolean sendList(List<T> eventList) {

        List<EventBody<T>> eventBodyList = eventList
                .stream()
                .map(event -> new EventBody<>(event, EventContext.get()))
                .collect(Collectors.toList());

        List<EventId> eventIdList = getTransactionSupport().execute(() -> getEventStorageService().saveList(eventBodyList));

        // 事务后触发发送
        getTransactionSupport().executeAfterCommit(() -> getAsyncSendExecutor().execute(() ->
                // 发送限流
                getEventTransferSenderLimitingControl().control(eventList, eventIdList, (eList, eIdList) -> {

                    // 发送消息
                    BatchSendResult batchSendResult = getTransferProducer().sendMessageList(eventBodyList, eIdList);

                    if (CollectionUtils.isNotEmpty(batchSendResult.getSendCompletedIndexList())) {
                        List<EventId> completedEventIdList = batchSendResult.getSendCompletedIndexList()
                                .stream()
                                .map(eventIdList::get)
                                .collect(Collectors.toList());
                        // 存储执行
                        getTransactionSupport().execute(() -> {
                            getEventStorageService().sendCompleteList(completedEventIdList);
                            return true;
                        });
                    }
                    if (CollectionUtils.isNotEmpty(batchSendResult.getSendFailedIndexList())) {
                        List<EventId> completedEventIdList = batchSendResult.getSendFailedIndexList()
                                .stream()
                                .map(eventIdList::get)
                                .collect(Collectors.toList());
                        // 存储执行
                        getTransactionSupport().execute(() -> {
                            getEventStorageService()
                                    .sendFailedList(completedEventIdList, batchSendResult.getFailedException());
                            return true;
                        });
                    }
                })));
        return true;
    }

    @Override
    public <T> boolean retrySend(EventId eventId, EventBody<T> eventBody) {

        BaseEventEntity eventEntity = getEventStorageService().getBaseEntity(eventId);

        if (getEventStorageService().isMoreThanMustTrigger(eventEntity) || eventEntity.isProcessComplete()) {
            log.warn("[RocketMqEventSender#retrySend] event-retry more than max-trigger!eventId:{}", eventId);
            return true;
        }

        getAsyncSendExecutor()
                .execute(() -> getEventTransferSenderLimitingControl().control(eventBody.getEvent(), eventId, (e, eId) -> {
                    // 发送消息
                    try {
                        getTransferProducer().sendMessage(eventBody, eId);
                    } catch (Throwable ex) {
                        getEventStorageService().sendFailed(eId, ex);
                        return;
                    }
                    // 存储执行
                    getTransactionSupport().execute(() -> {
                        getEventStorageService().sendComplete(eId);
                        return true;
                    });
                }));
        return true;
    }

}
