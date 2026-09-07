package com.hsc.common.domain;

import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class CallStats {
    private Audio audio;
}
