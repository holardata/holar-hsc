
package com.hsc.common.annotation;


import com.hsc.common.enums.RouteTypeEnum;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EslRouteName {

    RouteTypeEnum value();
}
