// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.constant;


public class EslConstant {

    public final static String SPACE = " ";

    public final static String SPLIT = ",";

    public final static String EQUAL = "=";

    public final static String EXCLAMATION  = "!";

    public static final String CO = ":";

    public final static String SET = "set";

    public final static String OK = "OK";

    public final static String SIP_HEADER = "sip_h_";

    public final static String SOFIA = "sofia";

    public final static String EXECUTE = "execute";

    public final static String PLAYBACK = "playback";

    public final static String LOOP_PLAYBACK = "loop_playback";

    public final static String BREAK_ = "break";

    public final static String HANGUP = "hangup";

    public final static String ANSWER = "answer";

    public final static String RING_READY = "ring_ready";

    /** AI 智能坐席腿的 agentName 标识：建 ai 腿时设、挂断/bridge 时按它识别 AI 腿（勿散落字面量） */
    public final static String AI_AGENT_NAME = "AI智能坐席";

    /**
     * 外呼被叫(客户手机)振铃超时秒数——sys_config 参数 call.ring-timeout.outbound 的代码默认值
     * (运行时以参数页配置为准，本常量仅在参数缺失时兜底；与 dml 预置值一致)。被叫是客户手机、
     * 响铃 15~25s 接听常见，10s 会掐掉慢接听。超时经 originate_timeout 由 FS 兜底挂断 B 腿，
     * 勿在 originate 变量加 return_ring_ready=true 否则该超时失效。
     */
    public final static int OUTBOUND_CALLEE_RING_TIMEOUT = 30;

    /**
     * 呼入转坐席/技能组轮呼/AI转人工/SIP 的 B 腿振铃超时秒数——sys_config 参数
     * call.ring-timeout.inbound-agent 的代码默认值。呼入侧坐席弹屏/轮呼场景 30s 宽裕
     * (轮呼超时即跳下一坐席，过长让排队客户干等)。
     */
    public final static int INBOUND_AGENT_RING_TIMEOUT = 30;

    /**
     * 呼入转座机(routeType=7)振铃超时秒数——sys_config 参数 call.ring-timeout.inbound-extension
     * 的代码默认值。座机要人走到桌边摘机，比坐席弹屏慢，60s 起步(过长则客户干听回铃)。
     */
    public final static int INBOUND_EXTENSION_RING_TIMEOUT = 60;

    /**
     * 技能组排队容量兜底值。queue_length DDL 可空(DEFAULT NULL)、前端表单必填，null 属
     * 异常数据(手工 SQL/脏数据)——兜底为温和默认：仍可排队、由排队超时(timeOut 未配兜底60s)
     * 挂断，不取 0(会立即溢出挂断客户，行为过激)。
     */
    public final static int CALL_SKILL_DEFAULT_QUEUE_LENGTH = 100;

    /** 技能组排队兜底排队音(FS sounds 内置)：技能组未配 queue_voice 时循环播放它，等待期持续有声 */
    public final static String CALL_SKILL_DEFAULT_QUEUE_VOICE = "queue.wav";

    /** 技能组排队周期播报间隔(毫秒)。sys_config 参数 call.queue-announce-interval-ms 的代码默认值 */
    public final static int CALL_SKILL_ANNOUNCE_INTERVAL_MS = 30_000;

    public final static String START = "start";

    public final static String BRIDGE = "uuid_bridge";

    public final static String UUID_KILL = "uuid_kill";

    public final static String RECORD = "uuid_record";

    public final static String SETVAR = "uuid_setvar";

    public final static String INTERNAL = "internal";

    public final static String EXTERNAL = "external";

    public final static String CONFERENCE = "conference";

    public final static String DETECT_SPEECH = "detect_speech";

    public final static String UNIMRCP = "unimrcp:";

    public final static String EAVESDROP = "eavesdrop";

    public final static String PARK = " &park()";


    public final static String ENABLE_FILE_WRITE_BUFFERING = " enable_file_write_buffering false";
    public final static String RECORD_CHECK_BRIDGE = " RECORD_CHECK_BRIDGE true";
    public final static String RECORD_USE_THREAD = " RECORD_USE_THREAD true";

    public final static String RECORD_SAMPLE_RATE = "record_sample_rate=";

    public final static String ORIGINATE = "originate";

    public final static String TRANSFER = "uuid_transfer";

    public final static String NORMAL_CLEARING = "NORMAL_CLEARING";

    public final static String PARK_AFTER_BRIDGE = "park_after_bridge=true";

    public final static String HANGUP_AFTER_BRIDGE = "hangup_after_bridge=false";

    public final static String PLAY_AND_GET_DIGITS = "play_and_get_digits";

    public final static String PLAYBACK_DELIMITER = "playback_delimiter=!";

    public final static String SEND_SILENCE_WHEN_IDLE ="send_silence_when_idle=";

    public final static String PLAYBACK_TERMINATORS = "playback_terminators=none";

    // ⚠️ 变量名必须是复数 playback_terminators（FS 官方变量，合法值 any/none/键集）；
    // 原单数 playback_terminator 写错变量名，开关打开也不生效（放音依旧打不断）
    public final static String PLAYBACK_TERMINATORS_ANY = "playback_terminators=any";

    public final static String SPEAK = "speak";

    public final static String PLAYBACK_SLEEP_VAL = "playback_sleep_val=0";

    private final static String PLAY_AND_DETECT_SPEECH = "play_and_detect_speech";

    public final static String FIRE_ASR_EVENT = "fire_asr_events=true";

    public final static String RESUME = "resume";
}
