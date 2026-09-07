package com.hsc.common.constant;

/**
 * sys_config 系统参数键常量：参数化改造(openspec sys-config-management)统一引用，
 * 杜绝 key 字符串散落各业务代码。默认值兜底仍在各调用点传参（常量仅锁 key）。
 *
 * <p>参数预置数据在 liquibase dml changeset 20260901-sys-config-preset（9 个，全数值型）；
 * 页面「系统管理→参数管理」仅可改 config_value 与 remark，不可增删。
 */
public class SysConfigKeys {

    /** 外呼被叫振铃超时(秒)，默认 25(EslConstant.OUTBOUND_CALLEE_RING_TIMEOUT) */
    public final static String RING_TIMEOUT_OUTBOUND = "call.ring-timeout.outbound";

    /** 呼入转坐席/技能组轮呼/AI转人工/SIP 的 B 腿振铃超时(秒)，默认 30 */
    public final static String RING_TIMEOUT_INBOUND_AGENT = "call.ring-timeout.inbound-agent";

    /** 呼入转座机(routeType=7)振铃超时(秒)，默认 60 */
    public final static String RING_TIMEOUT_INBOUND_EXTENSION = "call.ring-timeout.inbound-extension";

    /** 座机代拨第一段(坐席话机腿)振铃超时(秒)，默认 10(ICallServiceImpl.DESK_AGENT_RING_TIMEOUT) */
    public final static String DESK_DIAL_AGENT_RING_TIMEOUT = "call.desk-dial-agent-ring-timeout";

    /** 技能组排队超时兜底(秒)，默认 60(FsSkillGroupRouteHandler 字面量) */
    public final static String QUEUE_TIMEOUT_DEFAULT = "call.queue-timeout-default";

    /** 技能组排队容量兜底(人)，默认 100(EslConstant.CALL_SKILL_DEFAULT_QUEUE_LENGTH) */
    public final static String QUEUE_CAPACITY_DEFAULT = "call.queue-capacity-default";

    /** 漏接重分配次数上限，默认 1(FsChannelHangUpCompleteEslEventHandler) */
    public final static String MISSED_REASSIGN_LIMIT = "call.missed-reassign-limit";

    /** 技能组排队周期播报间隔(毫秒)，默认 30000(EslConstant.CALL_SKILL_ANNOUNCE_INTERVAL_MS) */
    public final static String QUEUE_ANNOUNCE_INTERVAL_MS = "call.queue-announce-interval-ms";

    /** 登录 token 有效期(分钟)，默认 720(CacheConstants.EXPIRATION)；仅影响修改后新登录 */
    public final static String TOKEN_EXPIRE_MINUTES = "session.token-expire-minutes";

    private SysConfigKeys() {
    }
}
