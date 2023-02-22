package org.svnee.easyevent.transfer.api.route;

import org.svnee.easyevent.common.model.Pair;

/**
 * 事件路由服务
 *
 * @author svnee
 */
public interface EventRouteStrategy {

    /**
     * 事件路由topic
     *
     * @param event event
     * @return 路由topic
     */
    Pair<String, String> route(Object event);

}
