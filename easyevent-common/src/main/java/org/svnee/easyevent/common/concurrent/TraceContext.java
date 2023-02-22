package org.svnee.easyevent.common.concurrent;

import static org.svnee.easyevent.common.utils.ParamUtils.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.svnee.easyevent.common.utils.MapUtils;

/**
 * TraceContext
 *
 * @author svnee
 **/
public class TraceContext {

    private static final ThreadLocal<Map<String, Object>> CURRENT_TRACE_CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private TraceContext() {
    }

    public static void setTraceContextMap(Map<String, Object> currentTraceContextMap) {
        CURRENT_TRACE_CONTEXT.set(currentTraceContextMap);
    }

    public static Map<String, Object> getTraceContextMap() {
        Map<String, Object> traceContextMap = CURRENT_TRACE_CONTEXT.get();
        return Objects.isNull(traceContextMap) ? Collections.emptyMap() : traceContextMap;
    }

    public static void clear() {
        CURRENT_TRACE_CONTEXT.remove();
    }

    private static Object get(TraceContextParam param) {

        checkNotNull(param);

        return CURRENT_TRACE_CONTEXT.get().get(param.getCode());
    }

    private static Object putIfAbsent(TraceContextParam param, Object value) {

        checkNotNull(param);
        checkNotNull(value);

        Map<String, Object> map = CURRENT_TRACE_CONTEXT.get();
        if (Objects.isNull(map)) {
            map = new HashMap<>();
        }

        return map.putIfAbsent(param.getCode(), value);
    }

    /**
     * 获取traceId
     *
     * @return traceId
     */
    public static String getTraceId() {
        Object traceId = get(TraceContextParam.TRACE_ID);
        return Objects.nonNull(traceId) ? (String) traceId : null;
    }

    /**
     * get sourceEventId
     *
     * @return sourceEventId
     */
    public static Long getSourceEventId() {
        Object traceId = get(TraceContextParam.SOURCE_EVENT_ID);
        return Objects.nonNull(traceId) ? (Long) traceId : null;
    }

    /**
     * putIfAbsent sourceEventId
     *
     * @param sourceEventId sourceEventId
     */
    public static Long putSourceEventIdIfAbsent(Long sourceEventId) {
        return (Long) putIfAbsent(TraceContextParam.SOURCE_EVENT_ID, sourceEventId);
    }

}
