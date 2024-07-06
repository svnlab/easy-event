package com.openquartz.easyevent.starter.soa.core;

import com.openquartz.easyevent.core.annotation.Subscribe;
import com.openquartz.easyevent.starter.soa.api.SoaEvent;

/**
 * SoaEventHandler
 *
 * @author svnee
 */
public class SoaEventHandler {

    private final SoaEventCenter soaEventCenter;

    public SoaEventHandler(SoaEventCenter soaEventCenter) {
        this.soaEventCenter = soaEventCenter;
    }

    @Subscribe(condition = "#event.soaIdentify==@easyEventProperties.getAppId()")
    public void handle(SoaEvent event) {
        soaEventCenter.publish(event);
    }

}
