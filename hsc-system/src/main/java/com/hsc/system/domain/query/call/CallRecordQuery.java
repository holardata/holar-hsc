package com.hsc.system.domain.query.call;

import com.hsc.system.domain.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CallRecordQuery extends BaseQuery {

    private Long callId;
}
