package com.hsc.system.domain.vo.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息（供前端 getUserInfo 使用，脱敏：不含密码/权限集）
 *
 * @author hsc
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer sex;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
