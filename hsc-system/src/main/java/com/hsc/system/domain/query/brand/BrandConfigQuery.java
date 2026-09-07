package com.hsc.system.domain.query.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 品牌配置保存入参（site_config 键值对的强类型投影）。
 * 字段为 null/空串均按"回退构建默认值"处理（一键恢复默认=全部传空保存）。
 */
@Schema
@Data
public class BrandConfigQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "应用名称(顶栏/登录页/标签页标题)")
    private String appName;

    @Schema(description = "logo图片文件id(sys_file.id,前端拼file/play访问)")
    private String logoFileId;

    @Schema(description = "页签图标文件id")
    private String faviconFileId;

    @Schema(description = "登录页大图文件id(右侧介绍区)")
    private String sloganImageFileId;

    @Schema(description = "登录页介绍标题")
    private String pageTitle;

    @Schema(description = "登录页介绍描述")
    private String pageDescription;

    @Schema(description = "登录框副标题")
    private String loginSubtitle;

    @Schema(description = "产品文档链接(头像下拉'文档'项跳转,空=隐藏该入口)")
    private String docUrl;

    @Schema(description = "文档入口开关(头像下拉'文档'项是否显示,null/缺省按true)")
    private Boolean docUrlEnabled;
}
