package com.openquartz.easyevent.core.publisher;

import com.openquartz.easyevent.common.utils.CollectionUtils;
import com.openquartz.easyevent.core.EventBus;
import com.openquartz.easyevent.core.dispatcher.DispatchInvokeResult;
import com.openquartz.easyevent.core.intreceptor.PublisherInterceptorChain;
import com.openquartz.easyevent.core.intreceptor.PublisherInterceptorContext;
import com.openquartz.easyevent.storage.model.EventContext;
import com.openquartz.easyevent.transfer.api.EventSender;

import java.util.List;
import java.util.stream.Collectors;

import static com.openquartz.easyevent.common.utils.ParamUtils.checkNotNull;

/**
 * @author svnee
 **/
public abstract class EventPublisherAdapter implements EventPublisher {

    /**
     * 直接调度触发EventBus
     *
     * @return event-bus
     */
    public abstract EventBus getDirectEventBus();

    /**
     * 获取事件发送
     *
     * @return 事件发送者
     */
    public abstract EventSender getEventSender();

    @Override
    public <T> boolean syncPublish(T event) {

        PublisherInterceptorContext context = new PublisherInterceptorContext();
        boolean publish = PublisherInterceptorChain.applyPrePublish(event, context);
        if (!publish) {
            return true;
        }

        // 触发EventBus
        EventBus directEventBus = getDirectEventBus();

        checkNotNull(directEventBus);

        DispatchInvokeResult invokeResult;
        try {
            invokeResult = directEventBus.post(event);
        } catch (Exception ex) {
            PublisherInterceptorChain.triggerAfterCompletion(event, context, ex);
            throw ex;
        }
        // 事件派发完成后触发 afterCompletion
        PublisherInterceptorChain.triggerAfterCompletion(event, context, null);
        return invokeResult.isSuccess();
    }

    @Override
    public <T> boolean asyncPublish(T event) {

        try {
            PublisherInterceptorContext publishContext = new PublisherInterceptorContext();
            boolean publish = PublisherInterceptorChain.applyPrePublish(event, publishContext);
            if (!publish) {
                return true;
            }
            boolean sendResult;
            try {
                sendResult = getEventSender().send(event);
            } catch (Exception ex) {
                PublisherInterceptorChain.triggerAfterCompletion(event, publishContext, ex);
                throw ex;
            }
            PublisherInterceptorChain.triggerAfterCompletion(event, publishContext, null);
            return sendResult;
        } finally {
            EventContext.remove();
        }
    }

    @Override
    public <T> boolean asyncPublishList(List<T> eventList) {

        try {
            PublisherInterceptorContext publishContext = new PublisherInterceptorContext();

            List<Object> filterEventList = eventList.stream()
                    .filter(event -> PublisherInterceptorChain.applyPrePublish(event, publishContext))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(filterEventList)) {
                return true;
            }

            boolean sendResult;
            try {
                sendResult = getEventSender().sendList(filterEventList);
            } catch (Exception ex) {
                for (Object event : filterEventList) {
                    PublisherInterceptorChain.triggerAfterCompletion(event, publishContext, ex);
                }
                throw ex;
            }
            for (Object event : filterEventList) {
                PublisherInterceptorChain.triggerAfterCompletion(event, publishContext, null);
            }
            return sendResult;
        } finally {
            EventContext.remove();
        }
    }
}
