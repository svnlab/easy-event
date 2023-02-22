package org.svnee.easyevent.transfer.api;

import java.util.List;
import org.svnee.easyevent.common.model.LifecycleBean;
import org.svnee.easyevent.storage.identify.EventId;

/**
 * Event Sender
 *
 * @author svnee
 */
public interface EventSender extends LifecycleBean {

    /**
     * 发送
     *
     * @param event 事件
     * @return 是否发送成功
     */
    <T> boolean send(T event);

    /**
     * 批量发布
     *
     * @param eventList 事件集合
     * @return 发布消息
     */
    <T> boolean sendList(List<T> eventList);

    /**
     * 重试发送
     *
     * @param eventId 事件消息
     * @param event event
     * @return 是否成功发送
     */
    <T> boolean retrySend(EventId eventId, T event);
}
