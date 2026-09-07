package com.hsc.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 外呼拨打结果枚举（唯一判定入口）
 *
 * <p>由通话事实（是否接通 + FS 挂机原因码）经 {@link #of(boolean, Integer)} 判定为七类之一，
 * 挂断回写（任务拨打/私海拨打）与后续 AI 外呼专项统一调用本入口，不散落 if-else。
 *
 * <p>接通判定优先于挂机原因：answerTime 非空（CHANNEL_BRIDGE 过）即接通。
 * 网关信号能力边界（如 HX4G 不勾"延迟发送接通消息"时拒接表现为超时）会导致拒接被
 * 映射为未接听，属预期行为不做修正。
 *
 * @author danmo
 * @date 2026/9/3
 */
@Getter
public enum CallResultEnum {
    CONNECTED(1, "接通"),
    NO_ANSWER(2, "未接听"),
    BUSY(3, "占线"),
    REJECTED(4, "拒接"),
    INVALID_NUMBER(5, "空号或停机"),
    UNREACHABLE(6, "关机或无法接通"),
    FAILED(7, "呼叫失败");

    private final Integer code;
    private final String message;

    CallResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * FS 挂机原因码 → 结果的分组映射（设计 D4 判定表）
     */
    private static final Map<Integer, CallResultEnum> CAUSE_MAPPING = new HashMap<>();

    static {
        // 未接听：无应答/无用户响应/主叫回铃期放弃(坐席取消)/未指定正常挂断
        put(FsHangupCauseEnum.NO_ANSWER);
        put(FsHangupCauseEnum.NO_USER_RESPONSE);
        put(FsHangupCauseEnum.ORIGINATOR_CANCEL);
        put(FsHangupCauseEnum.NORMAL_UNSPECIFIED);
        // 占线
        put(FsHangupCauseEnum.USER_BUSY);
        put(FsHangupCauseEnum.NORMAL_CIRCUIT_CONGESTION);
        put(FsHangupCauseEnum.SWITCH_CONGESTION);
        // 拒接
        put(FsHangupCauseEnum.CALL_REJECTED);
        // 空号或停机
        put(FsHangupCauseEnum.UNALLOCATED_NUMBER);
        put(FsHangupCauseEnum.NUMBER_CHANGED);
        put(FsHangupCauseEnum.INVALID_NUMBER_FORMAT);
        // 关机或无法接通
        put(FsHangupCauseEnum.SUBSCRIBER_ABSENT);
        put(FsHangupCauseEnum.DESTINATION_OUT_OF_ORDER);
        put(FsHangupCauseEnum.NETWORK_OUT_OF_ORDER);
        put(FsHangupCauseEnum.NORMAL_TEMPORARY_FAILURE);
        put(FsHangupCauseEnum.RECOVERY_ON_TIMER_EXPIRE);
    }

    private static void put(FsHangupCauseEnum cause) {
        CAUSE_MAPPING.put(cause.getCode(), byCauseName(cause));
    }

    private static CallResultEnum byCauseName(FsHangupCauseEnum cause) {
        return switch (cause) {
            case NO_ANSWER, NO_USER_RESPONSE, ORIGINATOR_CANCEL, NORMAL_UNSPECIFIED -> NO_ANSWER;
            case USER_BUSY, NORMAL_CIRCUIT_CONGESTION, SWITCH_CONGESTION -> BUSY;
            case CALL_REJECTED -> REJECTED;
            case UNALLOCATED_NUMBER, NUMBER_CHANGED, INVALID_NUMBER_FORMAT -> INVALID_NUMBER;
            case SUBSCRIBER_ABSENT, DESTINATION_OUT_OF_ORDER, NETWORK_OUT_OF_ORDER,
                 NORMAL_TEMPORARY_FAILURE, RECOVERY_ON_TIMER_EXPIRE -> UNREACHABLE;
            default -> FAILED;
        };
    }

    /**
     * 判定入口：接通优先；未映射原因码（媒体超时/网关故障/系统类等）兜底为呼叫失败
     *
     * @param answered        是否接通（answerTime 非空，即 CHANNEL_BRIDGE 过）
     * @param hangupCauseCode FS 挂机原因码（FsHangupCauseEnum.code），可空
     */
    public static CallResultEnum of(boolean answered, Integer hangupCauseCode) {
        if (answered) {
            return CONNECTED;
        }
        return CAUSE_MAPPING.getOrDefault(hangupCauseCode, FAILED);
    }

    public static String getMessage(Integer code) {
        for (CallResultEnum result : values()) {
            if (result.getCode().equals(code)) {
                return result.getMessage();
            }
        }
        return null;
    }
}
