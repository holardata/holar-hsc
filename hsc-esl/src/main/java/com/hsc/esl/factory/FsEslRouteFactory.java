// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.factory;

import com.hsc.common.annotation.EslRouteName;
import com.hsc.esl.handler.route.FsAbstractRouteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@Component
public class FsEslRouteFactory {

    private final Map<String, FsAbstractRouteHandler> routeMap = new ConcurrentHashMap<>(16);

    public FsEslRouteFactory(Map<String, FsAbstractRouteHandler> routeMap) {
        this.routeMap.putAll(routeMap);
    }

    public FsAbstractRouteHandler factory(Integer type) {
        for (FsAbstractRouteHandler handler : routeMap.values()) {
            EslRouteName routeName = handler.getClass().getAnnotation(EslRouteName.class);
            if (routeName == null) {
                routeName = handler.getClass().getSuperclass().getAnnotation(EslRouteName.class);
            }
            if (routeName == null) {
                // 未标 @EslRouteName 的 RouteHandler bean 直接 NPE(对齐 FsEslEventFactory 的守卫)
                continue;
            }
            if (Objects.equals(type, routeName.value().getType())) {
                return handler;
            }
        }
        return null;
    }

}
