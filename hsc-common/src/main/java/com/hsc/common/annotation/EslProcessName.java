package com.hsc.common.annotation;


import com.hsc.common.enums.ProcessEnum;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EslProcessName {

    ProcessEnum value();
}
