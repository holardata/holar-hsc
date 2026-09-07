package com.hsc.system.domain.query.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户自助修改密码入参（个人中心，验旧密码）。
 *
 * @author hsc
 */
@Schema
@Data
public class UserProfilePasswordQuery {

    @NotEmpty(message = "旧密码不能为空")
    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotEmpty(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度须为6-20位")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
