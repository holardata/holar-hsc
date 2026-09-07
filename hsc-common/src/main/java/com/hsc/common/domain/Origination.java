package com.hsc.common.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Origination {
    private OriginationCallerProfile originationCallerProfile;
}
