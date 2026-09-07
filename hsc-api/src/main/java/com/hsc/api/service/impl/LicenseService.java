package com.hsc.api.service.impl;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hsc.common.exception.CommonException;
import com.hsc.common.exception.LoginException;
import com.hsc.system.domain.vo.license.LicenseInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 海纳 license 许可证服务（licsrv）统一访问入口。
 * licsrv 为同宿主机部署的独立服务，本类收敛全部访问与响应解析（双重 JSON 字符串），
 * 供登录授权闸与 /auth/v1/license/* 代理接口共用。
 *
 * @date 2026-08-28
 */
@Slf4j
@Service
public class LicenseService {

    /**
     * 本产品在 licsrv 登记的产品名（licenseInfo 的 pn 参数，须与证内 pn 字段一致，如 holar_video_meet / holar_cc）
     */
    public static final String PRODUCT_NAME = "holar_cc";

    /**
     * license 专用错误码：前端据此识别授权失败并弹授权窗（现有码段 -1/100/101/1000+/226/433/505 均不冲突）
     */
    public static final int LICENSE_ERROR_CODE = 4001;

    /**
     * licsrv 同宿主机部署，3 秒快失败，避免拖死登录
     */
    private static final int TIMEOUT_MS = 3000;

    /**
     * licsrv 基地址（如 http://host.docker.internal:29000/licsrv，容器经 host-gateway 访问宿主机），
     * 环境变量 HSC_LICSRV_URL 注入
     */
    @Value("${hsc.licsrv.url}")
    private String licsrvUrl;

    /**
     * 获取机器码（供发放许可证时绑定机器）。网络类失败翻译为友好提示，
     * 原始异常（如 Connection refused）不直接透给前端
     */
    public String getMachinecode() {
        try {
            JSONObject resp = get("/license/getMachinecode");
            return resp.getString("data");
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("licsrv 机器码获取失败: {}", e.getMessage());
            throw new CommonException("授权服务不可达，请联系管理员");
        }
    }

    /**
     * 查询授权状态。不抛授权类异常：一切失败（未授权/过期/服务不可达）归 authorized=false + reason，
     * 供登录页预检与个人中心展示；登录强校验走 {@link #checkAuthorizedForLogin()}。
     */
    public LicenseInfoVo getLicenseInfo() {
        try {
            JSONObject resp = get("/license/licenseInfo?pn=" + PRODUCT_NAME);
            JSONObject info = parseLicenseInfo(resp.getString("data"));
            // licsrv 证结构：pn 是产品英文标识（匹配键），一证可授权多个产品，多个 pn 逗号分隔
            // （如 holar_video_meet,holar_cc）；additional 是授权产品显示名（中文，仅展示），授权判定只看证内 pn
            String pn = info.getString("pn");
            Long exp = info.getLong("exp");
            if (!pnMatches(pn)) {
                return LicenseInfoVo.builder().authorized(false).reason("系统未授权，请导入许可证").build();
            }
            if (exp == null) {
                return LicenseInfoVo.builder().authorized(false).reason("许可证信息异常，请重新授权").build();
            }
            long expireTime = exp * 1000;
            if (expireTime <= System.currentTimeMillis()) {
                return LicenseInfoVo.builder().authorized(false).expireTime(expireTime).reason("授权已过期，请更换许可证").build();
            }
            return LicenseInfoVo.builder().authorized(true).expireTime(expireTime).build();
        } catch (CommonException e) {
            // licsrv 已连通的业务应答错误（如「产品未授权」）或响应格式异常：透传真实原因，不误报"服务不可达"
            log.warn("licsrv 返回业务错误: {}", e.getMessage());
            return LicenseInfoVo.builder().authorized(false).reason(e.getMessage()).build();
        } catch (Exception e) {
            log.error("licsrv 授权信息查询失败: {}", e.getMessage());
            return LicenseInfoVo.builder().authorized(false).reason("授权服务不可达，请联系管理员").build();
        }
    }

    /**
     * 登录授权闸：未授权/已过期/licsrv 不可达一律拒绝登录（fail-closed），错误码 4001 供前端弹授权窗
     */
    public void checkAuthorizedForLogin() {
        LicenseInfoVo info = getLicenseInfo();
        if (!Boolean.TRUE.equals(info.getAuthorized())) {
            throw new LoginException(LICENSE_ERROR_CODE, info.getReason());
        }
    }

    /**
     * 导入（更换）许可证；licsrv 拒绝时抛业务异常（GlobalExceptionHandler 转 ResResult，msg 透传前端），
     * 网络类失败翻译为友好提示
     */
    public void reloadLicense(String licenseCode) {
        JSONObject body = new JSONObject();
        body.put("licenseCode", licenseCode);
        try {
            post("/license/reloadLicense?pn=" + PRODUCT_NAME, body.toJSONString());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("licsrv 许可证导入失败: {}", e.getMessage());
            throw new CommonException("授权服务不可达，请联系管理员");
        }
    }

    /**
     * 证内 pn 匹配：一证可授权多个产品（pn 逗号分隔），本产品 pn 在列表内即视为授权；兼容单值证
     */
    private boolean pnMatches(String pn) {
        if (StringUtils.isBlank(pn)) {
            return false;
        }
        return Arrays.stream(pn.split(",")).map(String::trim).anyMatch(PRODUCT_NAME::equals);
    }

    private JSONObject get(String path) {
        String body = HttpRequest.get(licsrvUrl + path).timeout(TIMEOUT_MS).execute().body();
        return checkResponse(body);
    }

    private JSONObject post(String path, String jsonBody) {
        String body = HttpRequest.post(licsrvUrl + path)
                .body(jsonBody)
                .contentType("application/json")
                .timeout(TIMEOUT_MS)
                .execute()
                .body();
        return checkResponse(body);
    }

    /**
     * licsrv 响应统一校验：外层 {code, msg, data}。
     * 成功码实测为 0（2026-08-28 联调确认：机器码接口成功返回 code:0；video-meeting 前端拦截器
     * `res.data.code || 200` 靠 falsy 短路恰好把 0 当成功），此处兼容 0 与 200；
     * 失败码如 3001（产品未授权），msg 透传
     */
    private JSONObject checkResponse(String body) {
        JSONObject resp = JSON.parseObject(body);
        Integer code = resp == null ? null : resp.getInteger("code");
        if (code == null || (code != 0 && code != 200)) {
            String msg = resp == null ? null : resp.getString("msg");
            throw new CommonException(StringUtils.isNotBlank(msg) ? msg : "许可证服务返回错误");
        }
        return resp;
    }

    /**
     * 解析 licsrv 的「双重 JSON 字符串」data（video-meeting 前端为 JSON.parse(JSON.parse(data))）：
     * 逐层解包直到得到 JSON 对象（兼容只包一层的情况）
     */
    private JSONObject parseLicenseInfo(String data) {
        String current = data;
        for (int i = 0; i < 2 && StringUtils.isNotBlank(current); i++) {
            String trimmed = current.trim();
            if (trimmed.startsWith("{")) {
                return JSON.parseObject(trimmed);
            }
            current = JSON.parseObject(trimmed, String.class);
        }
        throw new CommonException("许可证信息格式异常");
    }
}
