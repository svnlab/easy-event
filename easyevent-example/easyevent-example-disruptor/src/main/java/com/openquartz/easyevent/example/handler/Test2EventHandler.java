package com.openquartz.easyevent.example.handler;

import com.openquartz.easyevent.core.annotation.AllowConcurrentEvents;
import com.openquartz.easyevent.core.annotation.Subscribe;
import com.openquartz.easyevent.example.event.TestEvent;
import com.openquartz.easyevent.starter.annotation.EventHandler;

/**
 * @author svnee
 **/
@EventHandler
public class Test2EventHandler {

    @Subscribe
    @AllowConcurrentEvents
    public void handle(TestEvent event) {
        throw new RuntimeException("xxxx");
    }

}