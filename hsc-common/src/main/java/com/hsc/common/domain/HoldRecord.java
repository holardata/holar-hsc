package com.hsc.common.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;


@Data
@Accessors(chain = true)
public class HoldRecord {
    private List<Hold> holds;
}
