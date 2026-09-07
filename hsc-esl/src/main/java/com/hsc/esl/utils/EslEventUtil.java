// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.utils;


import org.freeswitch.esl.client.transport.event.EslEvent;

public class EslEventUtil {

    public static final String CORE_UUID = "Core-UUID";
    public static final String UNIQUE_ID = "Unique-ID";
    public static final String OTHER_UNIQUE_ID = "Other-Leg-Unique-ID";

    private static final String CALL_DIRECTION = "Call-Direction";
    public static final String EVENT_NAME = "Event-Name";
    public static final String EVENT_SEQUENCE = "Event-Sequence";
    public static final String EVENT_DATE_LOCAL = "Event-Date-Local";
    public static final String EVENT_DATE_GMT = "Event-Date-GMT";
    public static final String EVENT_DATE_TIMESTAMP = "Event-Date-Timestamp";

    public static final String EVENT_CALLING_FILE = "Event-Calling-File";
    public static final String EVENT_CALLING_FUNCTION = "Event-Calling-Function";
    public static final String EVENT_CALLING_LINE_NUMBER = "Event-Calling-Line-Number";

    public static final String FREESWITCH_IPV4 = "FreeSWITCH-IPv4";
    public static final String FREESWITCH_IPV6 = "FreeSWITCH-IPv6";
    public static final String FREESWITCH_HOSTNAME = "FreeSWITCH-Hostname";
    public static final String FREESWITCH_SWITCHNAME = "FreeSWITCH-Switchname";

    public static final String CALLER_UNIQUE_ID = "Caller-Unique-ID";
    public static final String CALLER_NETWORK_ADDR = "Caller-Network-address";

    public static final String CALL_CHANNEL_UUID = "Channel-Call-UUID";

    public static final String CALLER_CONTEXT = "Caller-Context";
    public static final String CALLER_DIALPLAN = "Caller-Dialplan";
    public static final String CALLER_DIRECTION = "Caller-Direction";
    public static final String CALLER_LOGICAL_DIRECTION = "Caller-Logical-Direction";
    public static final String CALLER_PROFILE_INDEX = "Caller-Profile-Index";

    public static final String CALLER_ANI = "Caller-ANI";
    public static final String APPLICATION = "Application";
    public static final String APPLICATION_RESPONSE = "Application-Response";
    public static final String APPLICATION_DATA = "Application-Data";



    public static final String CALLER_USERNAME = "Caller-Username";
    public static final String CALLER_DESTINATION_NUMBER = "Caller-Destination-Number";
    public static final String CALLER_CALLER_ID_NAME = "Caller-Caller-ID-Name";
    public static final String CALLER_CALLER_ID_NUMBER = "Caller-Caller-ID-Number";

    public static final String CALLER_ORIG_CALLER_ID_NAME = "Caller-Orig-Caller-ID-Name";
    public static final String CALLER_ORIG_CALLER_ID_NUMBER = "Caller-Orig-Caller-ID-Number";

    public static final String CALLER_PROFILE_CREATED_TIME = "Caller-Profile-Created-Time";

    public static final String CALLER_CHANNEL_CREATED_TIME = "Caller-Channel-Created-Time";
    public static final String CALLER_CHANNEL_PROGRESS_TIME = "Caller-Channel-Progress-Time";
    public static final String CALLER_CHANNEL_PROGRESS_MEDIA_TIME = "Caller-Channel-Progress-Media-Time";
    public static final String CALLER_CHANNEL_ANSWERED_TIME = "Caller-Channel-Answered-Time";
    public static final String CALLER_CHANNEL_HANGUP_TIME = "Caller-Channel-Hangup-Time";
    public static final String HANGUP_CAUSE = "Hangup-Cause";
    public static final String CALLER_CHANNEL_BRIDGED_TIME = "Caller-Channel-Bridged-Time";

    public static final String CALLER_CHANNEL_NAME = "Caller-Channel-Name";
    public static final String CALLER_CHANNEL_HOLD_ACCUM = "Caller-Channel-Hold-Accum";
    public static final String CALLER_CHANNEL_LAST_HOLD = "Caller-Channel-Last-Hold";
    public static final String CALLER_CHANNEL_TRANSFER_TIME = "Caller-Channel-Transfer-Time";
    public static final String CALLER_CHANNEL_RESURRECT_TIME = "Caller-Channel-Resurrect-Time";

    public static final String VARIABLE_SIP_TO_URI = "variable_sip_to_uri";

    public static final String VARIABLE_SIP_TO_HOST = "variable_sip_to_host";

    private static final String VARIABLE_SIP_VIA_PORT = "variable_sip_via_port";
    private static final String VARIABLE_SIP_CONTACT_PORT = "variable_sip_contact_port";

    public static final String VARIABLE_SIP_TO_REAL_USER = "variable_sip_h_X-To-Real-User";

    /**
     * 任务/私海拨打身份标识（软电话 JsSIP 自定义头透传，FS 对 inbound INVITE 的 X- 头
     * 存为 channel 变量，与 AI 链路 sip_h_X-HSC-CallId 同机制）：
     * 任务拨打带 Assignment/Task/Customer 三值，私海拨打仅带 Customer。
     * 变量名按 FS 标准行为，真机可读性以 openspec 变更 customer-pool-outbound-feedback 任务 1.1 实测为准。
     */
    public static final String VARIABLE_SIP_H_ASSIGNMENT_ID = "variable_sip_h_X-HSC-Assignment-Id";
    public static final String VARIABLE_SIP_H_TASK_ID = "variable_sip_h_X-HSC-Task-Id";
    public static final String VARIABLE_SIP_H_CUSTOMER_ID = "variable_sip_h_X-HSC-Customer-Id";

    public static final String VARIABLE_SIP_VOICE_GATEWAY = "variable_sip_h_X-Voice-Gateway";

    public static final String VARIABLE_SIP_USER_AGENT = "variable_sip_user_agent";
    public static final String VARIABLE_SIP_HANGUP_PHRASE = "variable_sip_hangup_phrase";
    public static final String VARIABLE_SIP_VIA_PROTOCOL = "variable_sip_via_protocol";
    public static final String VARIABLE_SIP_TERM_STATUS = "variable_sip_term_status";
    public static final String VARIABLE_CHANNEL_NAME = "variable_channel_name";
    public static final String VARIABLE_SIP_CONTACT_URI = "variable_sip_contact_uri";
    public static final String VARIABLE_SIP_REQ_PORT = "variable_sip_req_port";

    public static final String VARIABLE_CURRENT_APPLICATION_DATA = "variable_current_application_data";

    public static final String VARIABLE_MENU_DTMF_RETURN = "variable_MENU_DTMF_RETURN";

    private EslEventUtil() {
    }

    public static String getCoreUuid(EslEvent event) {
        return event.getEventHeaders().get(CORE_UUID);
    }

    public static String getUniqueId(EslEvent event) {
        return event.getEventHeaders().get(UNIQUE_ID);
    }

    public static String getOtherUniqueId(EslEvent event) {
        return event.getEventHeaders().get(OTHER_UNIQUE_ID);
    }

    public static String getCallerUniqueId(EslEvent event) {
        return event.getEventHeaders().get(CALLER_UNIQUE_ID);
    }

    public static String getCallChannelUuid(EslEvent event) {
        return event.getEventHeaders().get(CALL_CHANNEL_UUID);
    }

    public static String getEventName(EslEvent event) {
        return event.getEventHeaders().get(EVENT_NAME);
    }

    public static String getEventSequence(EslEvent event) {
        return event.getEventHeaders().get(EVENT_SEQUENCE);
    }

    public static String getEventDateLocal(EslEvent event) {
        return event.getEventHeaders().get(EVENT_DATE_LOCAL);
    }

    public static String getEventDateGmt(EslEvent event) {
        return event.getEventHeaders().get(EVENT_DATE_GMT);
    }

    public static String getEventCallingFile(EslEvent event) {
        return event.getEventHeaders().get(EVENT_CALLING_FILE);
    }

    public static String getEventCallingFunction(EslEvent event) {
        return event.getEventHeaders().get(EVENT_CALLING_FUNCTION);
    }

    public static String getEventCallingLineNumber(EslEvent event) {
        return event.getEventHeaders().get(EVENT_CALLING_LINE_NUMBER);
    }

    public static String getFreeswitchIpv4(EslEvent event) {
        return event.getEventHeaders().get(FREESWITCH_IPV4);
    }

    public static String getFreeswitchIpv6(EslEvent event) {
        return event.getEventHeaders().get(FREESWITCH_IPV6);
    }

    public static String getFreeswitchHostname(EslEvent event) {
        return event.getEventHeaders().get(FREESWITCH_HOSTNAME);
    }

    public static String getFreeswitchSwitchname(EslEvent event) {
        return event.getEventHeaders().get(FREESWITCH_SWITCHNAME);
    }

    public static String getEventDateTimestamp(EslEvent event) {
        return event.getEventHeaders().get(EVENT_DATE_TIMESTAMP);
    }

    public static String getCallerProfileCreatedTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_PROFILE_CREATED_TIME);
    }

    public static String getCallerChannelCreatedTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_CREATED_TIME);
    }

    public static String getCallerChannelProgressTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_PROGRESS_TIME);
    }

    public static String getCallerChannelProgressMediaTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_PROGRESS_MEDIA_TIME);
    }

    public static String getCallerChannelAnsweredTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_ANSWERED_TIME);
    }

    public static String getCallerChannelHangupTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_HANGUP_TIME);
    }

    public static String getHangupCause(EslEvent event) {
        return event.getEventHeaders().get(HANGUP_CAUSE);
    }

    public static String getCallerChannelBridgedTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_BRIDGED_TIME);
    }

    public static String getCallerNetworkAddr(EslEvent event) {
        return event.getEventHeaders().get(CALLER_NETWORK_ADDR);
    }

    public static String getCallerContext(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CONTEXT);
    }

    public static String getCallerDialplan(EslEvent event) {
        return event.getEventHeaders().get(CALLER_DIALPLAN);
    }

    public static String getCallerDirection(EslEvent event) {
        return event.getEventHeaders().get(CALLER_DIRECTION);
    }

    public static String getCallerLogicalDirection(EslEvent event) {
        return event.getEventHeaders().get(CALLER_LOGICAL_DIRECTION);
    }

    public static String getCallerProfileIndex(EslEvent event) {
        return event.getEventHeaders().get(CALLER_PROFILE_INDEX);
    }

    public static String getCallerChannelName(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_NAME);
    }

    public static String getCallerChannelHoldAccum(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_HOLD_ACCUM);
    }

    public static String getCallerChannelLastHold(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_LAST_HOLD);
    }

    public static String getCallerChannelTransferTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_TRANSFER_TIME);
    }

    public static String getCallerChannelResurrectTime(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CHANNEL_RESURRECT_TIME);
    }

    public static String getCallerAni(EslEvent event) {
        return event.getEventHeaders().get(CALLER_ANI);
    }

    public static String getCallerUsername(EslEvent event) {
        return event.getEventHeaders().get(CALLER_USERNAME);
    }

    public static String getCallerDestinationNumber(EslEvent event) {
        return event.getEventHeaders().get(CALLER_DESTINATION_NUMBER);
    }

    public static String getCallerCallerIdName(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CALLER_ID_NAME);
    }

    public static String getCallerCallerIdNumber(EslEvent event) {
        return event.getEventHeaders().get(CALLER_CALLER_ID_NUMBER);
    }

    public static String getCallerOrigCallerIdName(EslEvent event) {
        return event.getEventHeaders().get(CALLER_ORIG_CALLER_ID_NAME);
    }

    public static String getCallerOrigCallerIdNumber(EslEvent event) {
        return event.getEventHeaders().get(CALLER_ORIG_CALLER_ID_NUMBER);
    }

    public static String getSipToUri(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_TO_URI);
    }

    public static String getVoiceGateway(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_VOICE_GATEWAY);
    }

    public static String getToHost(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_TO_HOST);
    }

    public static String getSipViaPort(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_VIA_PORT);
    }

    public static String getSipContactPort(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_CONTACT_PORT);
    }

    public static String getToRealUser(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_TO_REAL_USER);
    }

    /**
     * 读任务/私海拨打身份标识（数字型 X 头），值缺失或非数字返回 null（普通话单零侵入兜底）
     */
    public static Long getSipHAssignmentId(EslEvent event) {
        return parseLongSafely(event.getEventHeaders().get(VARIABLE_SIP_H_ASSIGNMENT_ID));
    }

    public static Long getSipHTaskId(EslEvent event) {
        return parseLongSafely(event.getEventHeaders().get(VARIABLE_SIP_H_TASK_ID));
    }

    public static Long getSipHCustomerId(EslEvent event) {
        return parseLongSafely(event.getEventHeaders().get(VARIABLE_SIP_H_CUSTOMER_ID));
    }

    private static Long parseLongSafely(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public static String getVariableSipUserAgent(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_USER_AGENT);
    }

    public static String getSipHangupPhrase(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_HANGUP_PHRASE);
    }

    public static String getSipViaProtocol(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_VIA_PROTOCOL);
    }

    public static String getSipTermStatus(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_TERM_STATUS);
    }

    public static String getChannelName(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_CHANNEL_NAME);
    }


    public static String getCallDirection(EslEvent event) {
        return event.getEventHeaders().get(CALLER_DIRECTION);
    }

    public static String getSipContactUri(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_CONTACT_URI);
    }

    public static String getSipReqPort(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_SIP_REQ_PORT);
    }

    public static String getApplication(EslEvent event) {
        return event.getEventHeaders().get(APPLICATION);
    }
    public static String getApplicationResponse(EslEvent event) {
        return event.getEventHeaders().get(APPLICATION_RESPONSE);
    }

    public static String getApplicationData(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_CURRENT_APPLICATION_DATA);
    }

    public static String getMenuDtmfReturn(EslEvent event) {
        return event.getEventHeaders().get(VARIABLE_MENU_DTMF_RETURN);
    }

    /**
     * 取 play_and_get_digits 的按键结果：从事件头 Application-Data 解析本次收号的 var_name
     * （第 8 个空格分隔参数），精确读对应通道变量。不能用轮询——FS 通道变量不清除，
     * 菜单收过的 MENU_DTMF_RETURN 会残留，导致后续收号/满意度节点读到别人的旧值（串台）。
     * 解析不出 var_name 时返回 null（回调按"未按键"处理，安全兜底）。
     */
    public static String getDigitsReturn(EslEvent event) {
        String appData = event.getEventHeaders().get(APPLICATION_DATA);
        if (appData == null) {
            return null;
        }
        String[] parts = appData.trim().split("\\s+");
        // play_and_get_digits(min max tries timeout terminators file invalid_file var_name regexp ...)
        if (parts.length < 8 || parts[7].isEmpty()) {
            return null;
        }
        String value = event.getEventHeaders().get("variable_" + parts[7]);
        return value == null || value.isEmpty() ? null : value;
    }
}
