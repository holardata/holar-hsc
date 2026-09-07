package com.hsc.system.domain.vo.menu;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * vben 前端动态路由结构。
 * <p>
 * 由 {@code SysMenu} 转换而来，供前端 {@code accessMode=mixed} 直接交给 {@code generateAccessible}，
 * 无需再做结构映射。字段语义对齐 vben {@code RouteRecordStringComponent}。
 *
 * @author holar
 * @since 2026-07-21
 **/
@Schema(description = "vben 动态路由")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 路由 name：按完整 path 生成的 PascalCase 标识（如 /system/user -> SystemUser），全局唯一。
     */
    @Schema(description = "路由 name（PascalCase，按完整 path 生成）")
    private String name;

    /**
     * 路由 path：绝对路径，顶级目录如 /system，子级如 /system/user。
     */
    @Schema(description = "路由 path（绝对路径）")
    private String path;

    /**
     * 前端组件路径：views 下相对路径（如 system/user/list）。
     * 目录（menuType=M）留空，由前端按首子路由推导 redirect；按钮（F）在转换前已剔除。
     */
    @Schema(description = "前端组件路径（views 下相对路径，目录为空）")
    private String component;

    /**
     * 重定向：留空，由前端对「有绝对 path 首子路由」的目录自动推导。
     */
    @Schema(description = "重定向（留空，前端自动推导）")
    private String redirect;

    /**
     * 路由元信息。
     */
    @Schema(description = "路由元信息")
    private Meta meta;

    /**
     * 子路由。
     */
    @Schema(description = "子路由")
    private List<RouterVo> children;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 菜单标题（来自 sys_menu.menu_name）。
         */
        @Schema(description = "菜单标题")
        private String title;

        /**
         * 图标：iconify 名（如 mdi:user）。
         */
        @Schema(description = "图标（iconify 名）")
        private String icon;

        /**
         * 排序（来自 sys_menu.order_num）。
         */
        @Schema(description = "排序")
        private Integer order;

        /**
         * 是否在菜单中隐藏（sys_menu.visible=1 时为 true）。
         */
        @Schema(description = "是否在菜单中隐藏")
        private Boolean hideInMenu;

        /**
         * 外链 URL（sys_menu.is_frame=0 外链时填 path）。
         */
        @Schema(description = "外链 URL")
        private String link;
    }
}
