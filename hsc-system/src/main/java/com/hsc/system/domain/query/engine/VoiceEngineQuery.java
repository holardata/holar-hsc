package com.hsc.system.domain.query.engine;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema
@Data
public class VoiceEngineQuery extends BaseQuery {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "ID", hidden = true)
    private List<Long> ids;

    @Schema(description = "引擎实例名称(模糊)")
    private String name;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "大类过滤 asr/tts")
    private String kind;
}
