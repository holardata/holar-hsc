// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.client;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.enums.EslEventFormat;
import com.hsc.common.enums.GatewayTypeEnum;
import com.hsc.common.thread.ThreadFactoryImpl;
import com.hsc.esl.FsEslMsg;
import com.hsc.esl.propeties.FsClientProperties;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.esl.service.IFsEslEventService;
import com.hsc.system.domain.entity.FsConfig;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.fsconfig.FsConfigQuery;
import com.hsc.system.service.IFsConfigService;
import com.hsc.system.service.IKoSubscriberService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author danmo
 * @date 2023年06月29日 9:49
 */
@Slf4j
public class FsClient {

    //呼叫编码
    @Value("${freeswitch.codec:^^:G722:PCMU:PCMA}")
    private String codec;

    @Value("${freeswitch.sample.rate:8000}")
    protected String sampleRate;

    @Value("${freeswitch.group:}")
    private String groupName;

    // 🆕 3.2 旁路 ASR fork（FS mod_audio_fork 把人工通话单方音频 WS 推流给 ai-callbot 做流式转写）。
    // 实时转写/AI推荐/挂断摘要与座机通话前端视图(CALL_STATUS)都依赖本链路；ai-callbot 是部署必备
    // 组件、mod_audio_fork 已编入 FS 镜像，故障时 fork 连不上只刷 FS 错误日志不伤通话。
    /** ai-callbot 旁路 WS 接收地址（mod_audio_fork 推流目标；URL query 由代码拼 callId/role/agentId） */
    @Value("${hsc.bypass.asr-bot-ws:ws://127.0.0.1:9091/asr/bypass}")
    private String bypassAsrBotWs;

    /** ai-callbot SIP 接听地址（IP:port），AI 智能坐席路由的 originate 目标（无注册 IP 直连 UAS） */
    @Value("${hsc.ai-callbot.sip-uri:}")
    private String aiCallbotSipUri;

    /**
     * FS 放音目录（FS 容器内 sound_prefix 绝对路径）。自编译 FS 对相对路径只拼语言目录
     * (sounds/en/us/callie/...)不查 sound_prefix 直拼，故放音文件统一在此拼成绝对路径下发。
     */
    @Value("${system.setting.fsSoundsDir:/usr/local/freeswitch/share/freeswitch/sounds}")
    private String fsSoundsDir;


    private Map<String, Client> fsClientMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService checkFsThread = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("check-fs-pool-"));


    private final FsClientProperties clientProperties;
    private final IFsConfigService iFsConfigService;


    @Autowired
    private IFsEslEventService ifsEslEventService;

    @Autowired
    protected IFsCallCacheService fsCallCacheService;

    @Autowired
    private IKoSubscriberService koSubscriberService;

    public FsClient(IFsConfigService iFsConfigService, FsClientProperties clientProperties) {
        this.iFsConfigService = iFsConfigService;
        this.clientProperties = clientProperties;
    }


    public void init() {
        // 不按 group 过滤:连所有 fs_config(支持一个后端管多台 FS;group 退化为纯 UI 标记)
        List<FsConfig> fsConfigs = iFsConfigService.getList(new FsConfigQuery());
        for (FsConfig fsConfig : fsConfigs) {
            connect(fsConfig);
        }
        checkFsThread.scheduleAtFixedRate(() -> {
            try {
                checkConnect();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 30, 15, TimeUnit.SECONDS);

    }

    public void connect(FsConfig server) {
        String ipAndPort = server.getIp() + EslConstant.CO + server.getPort();
        Client client = new Client();
        //监听事件
        client.addEventListener(new IEslEventListener() {

            @Override
            public void eventReceived(EslEvent event) {
                sendRocketMqMsg(event);
            }

            @Override
            public void backgroundJobResultReceived(EslEvent event) {
                sendRocketMqMsg(event);
            }

            private void sendRocketMqMsg(EslEvent event) {
                FsEslMsg lfsEslMsg = FsEslMsg.builder().eslEvent(event).address(ipAndPort).build();
                ifsEslEventService.eslEventPublisher(lfsEslMsg);
            }
        });
        //创建连接
        try {
            client.connect(server.getIp(), server.getPort(), server.getPassword(), server.getOutTime());
            client.setEventSubscriptions(EslEventFormat.PLAIN.getText(), "all");
            fsClientMap.put(ipAndPort, client);
        } catch (InboundConnectionFailure e) {
            log.error("Connect failed msg:{}", e.getMessage(), e);
        }
    }

    /**
     * fs连接状态检查
     */
    private void checkConnect() {
        List<FsConfig> fsConfigs = iFsConfigService.getList(new FsConfigQuery());
        for (FsConfig server : fsConfigs) {
            String clientUrl = server.getIp() + EslConstant.CO + server.getPort();
            if (fsClientMap.containsKey(clientUrl)) {
                Client client = fsClientMap.get(clientUrl);
                if (!client.canSend()) {
                    fsClientMap.remove(clientUrl);
                    client.close();
                    // ESL 断连（FS 重启等）：移除后立即重连，不等下一轮（否则要再等一个检查周期）
                    log.info("FS ESL 连接断开(可能 FS 重启)，立即重连 {}", clientUrl);
                    connect(server);
                }
            } else {
                connect(server);
            }
        }
    }

    /**
     * FS 是否已连接(address = ip:port,实时查内存 fsClientMap)
     */
    public boolean isConnected(String address) {
        return getConnectedClient(address) != null;
    }


    /**
     * 主动断开指定 FS 的 ESL 连接(主机配置删除时调用,address = ip:port)
     */
    public void disconnect(String address) {
        Client client = fsClientMap.remove(address);
        if (client != null && client.canSend()) {
            client.close();
        }
    }

    public void destroy() {
        log.info("fsClient 实例关闭链接");
        // 先停检查线程再清连接：clear 后 checkConnect 周期任务若仍在跑，会走 else 分支 connect()
        // 重建连接（停机后连接复活）；shutdown 只挡后续调度，正在执行的任务照常完成，故短暂等待
        checkFsThread.shutdown();
        try {
            if (!checkFsThread.awaitTermination(3, TimeUnit.SECONDS)) {
                log.warn("fsClient 连接检查线程未在3s内退出，继续关闭连接");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fsClientMap.forEach((path, client) -> {
            if (client.canSend()) {
                client.close();
            }
        });
        fsClientMap.clear();
        ifsEslEventService.destroyThreadPool();
    }


    /**
     * 发送消息
     */
    public void sendMsg(String address, SendMsg msg) {
        Client client = getConnectedClient(address);
        if (client != null) {
            client.sendMessage(msg);
        }
    }

    /**
     * 异步发送消息
     */
    public String sendAsyncMsg(String address, String cmd, String args) {
        Client client = getConnectedClient(address);
        if (client != null) {
            log.info("fs 发送异步消息 {}+{}", cmd, args);
            return client.sendAsyncApiCommand(cmd, args);
        }
        return null;
    }

    /**
     * 发送消息
     */
    public EslMessage sendSyncMsg(String address, String cmd, String args) {
        Client client = getConnectedClient(address);
        if (client != null) {
            return client.sendSyncApiCommand(cmd, args);
        }
        return null;
    }

    /**
     * 取指定地址已连接的 ESL client；未连接/已断开返回 null（各 send* 方法统一守卫）
     */
    private Client getConnectedClient(String address) {
        Client client = fsClientMap.get(address);
        return client != null && client.canSend() ? client : null;
    }

    /**
     * 取第一条 FS 的 ESL 地址(ip:port);未配置返回 empty。
     * 供"找一台 FS 发命令"的场景统一调用,避免各处重复查 fs_config。
     */
    public Optional<String> getFirstFsAddress() {
        List<FsConfig> configs = iFsConfigService.getList(new FsConfigQuery());
        if (configs == null || configs.isEmpty()) {
            return Optional.empty();
        }
        FsConfig fs = configs.get(0);
        return Optional.of(fs.getIp() + EslConstant.CO + fs.getPort());
    }

    /**
     * 通知 FS reloadxml:重新加载 XML 配置,触发 xml_curl 重新拉取动态配置(ACL 等 configuration 类)。
     * 用于后端 ACL 改动后让 FS 立即生效,免重启。FS 未连接仅 warn 不抛(数据已入库,
     * FS 恢复后下次操作或重启仍会同步)。
     */
    public void reloadXml() {
        try {
            Optional<String> address = getFirstFsAddress();
            if (address.isEmpty()) {
                log.warn("reloadxml 跳过:未配置FS主机(fs_config 为空)");
                return;
            }
            EslMessage msg = sendSyncMsg(address.get(), EslEventNames.RELOADXML, "");
            if (msg == null) {
                log.warn("reloadxml 未送达:FS 未连接或无响应 address={}", address.get());
            }
        } catch (Exception e) {
            log.error("reloadxml 异常,不阻断业务保存(数据已入库)", e);
        }
    }

    /**
     * 通知 FS reloadacl:重建 ACL 池(新版 FS 内部会先 reloadxml,触发 xml_curl 重新拉 fs_acl)。
     * ACL 改动后必须用 reloadacl 才能让 FS 内存里的 ACL 真正更新——单 reloadxml 只刷新 XML 树、
     * 不重建 ACL 池,改动不生效(踩坑:ACL 接口一直调 reloadXml 致前端改 ACL 无反应)。
     * FS 未连接仅 warn 不抛(数据已入库,FS 恢复后下次操作或重启仍会同步)。
     */
    public void reloadAcl() {
        try {
            Optional<String> address = getFirstFsAddress();
            if (address.isEmpty()) {
                log.warn("reloadacl 跳过:未配置FS主机(fs_config 为空)");
                return;
            }
            EslMessage msg = sendSyncMsg(address.get(), "reloadacl", "");
            if (msg == null) {
                log.warn("reloadacl 未送达:FS 未连接或无响应 address={}", address.get());
            }
        } catch (Exception e) {
            log.error("reloadacl 异常,不阻断业务保存(数据已入库)", e);
        }
    }

    /**
     * 通知 FS sofia profile rescan:重新扫描网关。
     * 网关分布在 internal(type=1)/external(type=2) 两个 profile,均 rescan 覆盖。
     * 用于 fs_sip_gateway 增删改后让 FS 立即生效,免重启。FS 未连接仅 warn 不抛。
     */
    public void sofiaRescan() {
        try {
            Optional<String> address = getFirstFsAddress();
            if (address.isEmpty()) {
                log.warn("sofia rescan 跳过:未配置FS主机(fs_config 为空)");
                return;
            }
            sendSyncMsg(address.get(), "sofia", "profile internal rescan");
            sendSyncMsg(address.get(), "sofia", "profile external rescan");
            log.info("sofia rescan internal+external address={}", address.get());
        } catch (Exception e) {
            log.error("sofia rescan 异常,不阻断业务保存(数据已入库)", e);
        }
    }

    /**
     * uuid应答
     *
     * @param address
     * @param uniqueId
     */
    public void answer(String address, String uniqueId) {
        SendMsg answer = new SendMsg(uniqueId);
        answer.addCallCommand(EslConstant.EXECUTE);
        answer.addExecuteAppName(EslConstant.ANSWER);
        sendMsg(address, answer);
    }

    /**
     * 标记通道振铃就绪（ring_ready app）：FS 向该腿的 SIP 对端发送 180 Ringing。
     * <p>晚应答架构下坐席腿 INVITE 进 FS 即 park，出局腿(B腿)独立 originate，bridge 前两腿无信令
     * 关联——B 腿收到网关的 180/183 不会转发给坐席腿，软电话打出后全程静默。B 腿 progress 时
     * 对坐席腿补发本命令，软电话由此触发 progress 事件播放本地回铃音。
     *
     * @param address FS 地址
     * @param uniqueId 坐席腿(主叫腿) uuid
     */
    public void ringReady(String address, String uniqueId) {
        SendMsg ringMsg = new SendMsg(uniqueId);
        ringMsg.addCallCommand(EslConstant.EXECUTE);
        ringMsg.addExecuteAppName(EslConstant.RING_READY);
        sendMsg(address, ringMsg);
    }

    /**
     * 挂机
     *
     * @param address
     * @param callId
     * @param uniqueId
     */
    public void hangupCall(String address, Long callId, String uniqueId) {
        if (StringUtils.isBlank(address) || StringUtils.isBlank(uniqueId)) {
            log.info("address:{} or uniqueId:{} is null", address, uniqueId);
            return;
        }

        SendMsg hangupMsg = new SendMsg(uniqueId);
        hangupMsg.addCallCommand(EslConstant.EXECUTE);
        hangupMsg.addExecuteAppName(EslConstant.HANGUP);
        hangupMsg.addExecuteAppArg(EslConstant.NORMAL_CLEARING);
        log.info("hangup call:{}, uniqueId:{}", callId, uniqueId);
        this.sendMsg(address, hangupMsg);
    }

    /**
     * 录音
     *
     * @param address
     * @param callId
     * @param uniqueId
     * @param filePath
     */
    public void record(String address, Long callId, String uniqueId, String filePath) {
        try {
            //设置8kHz采样率
            sendArgs(address, uniqueId, EslConstant.SET, EslConstant.RECORD_SAMPLE_RATE + sampleRate);
            //关闭缓存
            //sendSyncMsg(address, EslConstant.SETVAR, uniqueId + EslConstant.ENABLE_FILE_WRITE_BUFFERING);
            //桥接后录音
            sendSyncMsg(address, EslConstant.SETVAR, uniqueId + EslConstant.RECORD_CHECK_BRIDGE);
            //开启线程录音
            //sendSyncMsg(address, EslConstant.SETVAR, uniqueId + EslConstant.RECORD_USE_THREAD);
            sendSyncMsg(address, EslConstant.RECORD, uniqueId + " " + EslConstant.START + " " + filePath);
            log.info("FS开始录音 callId:{}, uniqueId:{}, record:{}", callId, uniqueId, filePath);
        } catch (Exception e) {
            log.error("FS开始录音异常 callId:{}, uniqueId:{}, ex:{}", callId, uniqueId, e.getMessage(), e);
        }
    }

    /**
     * 桥接
     *
     * @param address
     * @param callId
     * @param uniqueId
     * @param otherUniqueId
     */
    public void bridgeCall(String address, Long callId, String uniqueId, String otherUniqueId) {
        sendArgs(address, uniqueId, EslConstant.SET, EslConstant.PARK_AFTER_BRIDGE);
        sendArgs(address, uniqueId, EslConstant.SET, EslConstant.HANGUP_AFTER_BRIDGE);
        sendArgs(address, otherUniqueId, EslConstant.SET, EslConstant.HANGUP_AFTER_BRIDGE);
        sendArgs(address, otherUniqueId, EslConstant.SET, EslConstant.PARK_AFTER_BRIDGE);
        sendAsyncMsg(address, EslConstant.BRIDGE, uniqueId + EslConstant.SPACE + otherUniqueId);
    }

    /**
     * 强制挂断指定通道（uuid_kill）。AI 转人工时挂断 ai 腿释放媒体，再 originate 坐席腿 bridge。
     * 区别 hangupCall：hangupCall 给 park 状态的 leg 发 EXECUTE hangup，对已 bridge 的腿可能无效；
     * uuid_kill 按 uuid 直接终止 channel，任意状态都有效。
     */
    public void uuidKill(String address, String uniqueId, String cause) {
        sendAsyncMsg(address, EslConstant.UUID_KILL,
                uniqueId + EslConstant.SPACE + (StringUtils.isBlank(cause) ? EslConstant.NORMAL_CLEARING : cause));
    }

    /**
     * 给指定通道设置变量（uuid_setvar）。如 AI 转人工时给主叫腿设 send_silence_when_idle=1，
     * 让 ai 腿 kill、坐席腿尚未 bridge 之间发舒适噪音（避免主叫听到死寂）。
     */
    public void uuidSetvar(String address, String uniqueId, String var, String value) {
        sendAsyncMsg(address, EslConstant.SETVAR, uniqueId + EslConstant.SPACE + var + EslConstant.SPACE + value);
    }

    /**
     * 随机取一个已连接的 FS 实例地址（fsClientMap 的 key）。供需要指定 address 的调用方
     * （如 AiTransferService 的 uuidKill + makeCall 要用同一 FS 实例处理一个 call）。
     */
    public String getRandomAddress() {
        if (fsClientMap.isEmpty()) {
            return null;
        }
        return RandomUtil.randomEle(new ArrayList<>(fsClientMap.keySet()));
    }

    /**
     * update call
     *
     * @param address
     * @param uniqueId
     * @param name
     * @param arg
     */
    public void sendArgs(String address, String uniqueId, String name, String arg) {
        SendMsg msg = new SendMsg(uniqueId);
        msg.addCallCommand(EslConstant.EXECUTE);
        msg.addExecuteAppName(name);
        msg.addExecuteAppArg(arg);
        msg.addGenericLine("async", "true");
        sendMsg(address, msg);
    }


    /**
     * 创建呼叫
     *
     * @param callId        呼叫ID
     * @param called        呼叫号码
     * @param calledDisplay 呼叫显号
     * @param uniqueId      通道ID
     * @param timeOut       超时时间
     * @param callRoute     路由信息
     */
    public void makeCall(Long callId, String called, String calledDisplay, String uniqueId, Integer timeOut, FsSipGateway callRoute) {
        if (StringUtils.isBlank(called)) {
            log.warn("called:{} is null ", called);
            return;
        }
        String address = getRandomAddress();
        if (address == null) {
            log.warn("makeCall 跳过:无可用 FS ESL 连接 callId:{} called:{}", callId, called);
            return;
        }
        makeCall(address, callId, called, calledDisplay, uniqueId, timeOut, callRoute);
    }

    public void makeCall(String address, Long callId, String called, String calledDisplay, String uniqueId, Integer timeOut, FsSipGateway callRoute) {
        if (StringUtils.isBlank(called)) {
            log.warn("called:{} is null ", called);
            return;
        }

        //获取路由网关地址；出局外线网关(gatewayType=1)按 dial_prefix 给被叫加出局前缀，转坐席/空前缀不加
        String dialPrefix = callRoute.getDialPrefix();
        String routedCalled = (Objects.equals(callRoute.getGatewayType(), 1)
                && StringUtils.isNotBlank(dialPrefix))
                ? dialPrefix + called
                : called;
        String destination = routedCalled + Constants.AT + callRoute.getRealm();
        StringBuilder builder = originateVars(calledDisplay, uniqueId, timeOut, true);

        CallInfo callInfo = fsCallCacheService.getCallInfo(callId);
        boolean isInbound = Objects.nonNull(callInfo)
                && Objects.equals(DirectionEnum.INBOUND.getType(), callInfo.getDirection());
        // media_webrtc=true 只对 WebRTC 软电话(terminalType=0)加:让 FS 发 DTLS/ICE/SAVPF,软电话才能协商。
        // 座机(terminalType=1)不支持 WebRTC,带了会被 FS 当 WebRTC peer 呼 → 488 INCOMPATIBLE_DESTINATION。
        // 仅「呼入转坐席」(被叫是注册分机)时按终端类型区分;外呼被叫是外线号码(查不到)按非 WebRTC 处理。
        if (isInbound && isWebRtcTerminal(called)) {
            builder.append(",").append("media_webrtc=true");
        }

        // 按 fs_sip_gateway.type 决定 originate endpoint：
        // 转坐席 type=1(internal) → user/分机@realm：FS 查注册表直拨软电话 Contact，跳过 dialplan
        //   （sofia/internal/1411@realm 是 IP 直投，INVITE 到 5060 会被 dialplan park 拦截 answer，
        //    软电话收不到 INVITE；user/ 直接查注册表发给软电话 Contact，不进 dialplan）；
        // 出局 type=2(external) → sofia/external/号码@realm 出局（HX4G）。
        builder.append("}");
        if (Objects.equals(callRoute.getType(), GatewayTypeEnum.INTERNAL.getType())) {
            builder.append("user/").append(destination);
        } else {
            builder.append(EslConstant.SOFIA + "/")
                    .append(GatewayTypeEnum.EXTERNAL.getDesc())
                    .append("/")
                    .append(destination);
        }
        builder.append(EslConstant.PARK);
        sendAsyncMsg(address, EslConstant.ORIGINATE, builder.toString());
    }

    /**
     * 创建呼叫到 ai-callbot（AI 智能坐席路由专用）。
     * <p>ai-callbot 是无注册的 SIP UAS（IP 直连），endpoint 拼 {@code sofia/internal/sip:<ip>:<port>}，
     * 不走 user/@realm（ai-callbot 不注册，user/ 查注册表查不到）。带 {@code sip_h_X-HSC-CallId} 自定义
     * header 透传 hsc 的 callId 给 ai-callbot —— 话单合并生死线：ai-callbot 必须用它作 redis_call_id，
     * 否则 hsc 收到的 Redis 消息 callId 对不上、open/ASR/AI回复全落不进库。不带 media_webrtc
     * （ai-callbot 是 RTP G.711 UAS，非 WebRTC peer；codec 由 absolute_codec_string 与其 SDP 协商取 PCMU/PCMA）。
     *
     * @param address       FS 地址
     * @param callId        hsc 呼叫ID（雪花），透传给 ai-callbot 作 redis_call_id
     * @param uniqueId      新 ai 腿的 origination_uuid
     * @param callerDisplay 主叫显号（作 origination caller_id）
     * @param timeOut       振铃超时秒（可空）
     */
    public void makeCallToAiBot(String address, Long callId, String uniqueId, String callerDisplay, Integer timeOut) {
        if (StringUtils.isBlank(aiCallbotSipUri)) {
            log.error("originate 到 ai-callbot 失败：未配置 hsc.ai-callbot.sip-uri，callId:{}", callId);
            return;
        }
        StringBuilder builder = originateVars(callerDisplay, uniqueId, timeOut, false)
                .append(",").append(EslConstant.SIP_HEADER).append("X-HSC-CallId=").append(callId);
        builder.append("}")
                .append(EslConstant.SOFIA).append("/").append(EslConstant.INTERNAL)
                .append("/sip:").append(aiCallbotSipUri)
                .append(EslConstant.PARK);
        log.info("originate 到 ai-callbot callId:{} uri:{} aiLegUniqueId:{}", callId, aiCallbotSipUri, uniqueId);
        sendAsyncMsg(address, EslConstant.ORIGINATE, builder.toString());
    }

    /**
     * 拼 originate 命令的公共变量段（三个 makeCall* 共用；含左花括号、不含右花括号）。
     * <p>勿加 return_ring_ready=true——它使 originate 在收到 180/183(回铃/彩铃)时即视为成功返回，
     * originate_timeout 随之失效(拒接/无应答将干等网关自身超时，实测 61s)，且"回铃当接通"属
     * 电信行业伪应答监督(FAS)。接通判定只认 200 OK。
     *
     * @param display  显号（作 sip_contact_user 与 origination_caller_id_*）
     * @param uniqueId 新腿的 origination_uuid
     * @param timeOut  振铃超时秒（null 则不设，FS 默认 60s）
     * @param ringAsr  是否带彩铃检测变量 ring_asr/fire_asr_events（出局外线需要；ai 腿本机 UAS 不需要）
     */
    private StringBuilder originateVars(String display, String uniqueId, Integer timeOut, boolean ringAsr) {
        StringBuilder builder = new StringBuilder("{sip_contact_user=").append(display);
        if (ringAsr) {
            builder.append(",ring_asr=true,fire_asr_events=true");
        }
        builder.append(",absolute_codec_string=").append(codec)
                .append(",origination_caller_id_number=").append(display)
                .append(",origination_caller_id_name=").append(display)
                .append(",origination_uuid=").append(uniqueId);
        if (timeOut != null) {
            builder.append(",originate_timeout=").append(timeOut);
        }
        return builder;
    }

    /**
     * 被叫号码是否为 WebRTC 软电话终端(terminal_type:0-软电话 1-座机)。
     * <p>查不到(外线号码 / 数据缺失)按非 WebRTC 处理,避免对普通终端带 media_webrtc=true 致 488。
     * terminal_type 为 null 时按软电话(0)处理,与 KoSubscriberServiceImpl 新增默认值一致。
     */
    private boolean isWebRtcTerminal(String calleeNumber) {
        if (StringUtils.isBlank(calleeNumber)) {
            return false;
        }
        KoSubscriber subscriber = koSubscriberService.getByUserName(calleeNumber);
        if (subscriber == null) {
            return false;
        }
        Integer terminalType = subscriber.getTerminalType();
        return terminalType == null || terminalType == 0;
    }

    public void makeCall(String address, Long callId, String called, String calledDisplay, String uniqueId, Integer timeOut, String gatewayAddress) {
        if (StringUtils.isBlank(called)) {
            log.warn("called:{} is null ", called);
            return;
        }

        //获取路由网关地址
        String destination = called + Constants.AT + gatewayAddress;
        StringBuilder builder = originateVars(calledDisplay, uniqueId, timeOut, true);
        builder.append("}").append(EslConstant.SOFIA + "/").append(GatewayTypeEnum.EXTERNAL.getDesc()).append("/").append(destination).append(EslConstant.PARK);
        sendAsyncMsg(address, EslConstant.ORIGINATE, builder.toString());
    }

    /**
     * 🆕 旁路 ASR：FS mod_audio_fork 把指定 leg 的单方音频 WS 推流给 ai-callbot 转写（C 方案）。
     *
     * <p>替代原 eavesdrop SIP fork（混音致说话人分离失败）。mod_audio_fork 对 caller leg /
     * agent leg 各起一路推单方音频，ai-callbot 按 role 区分说话人。hsc 在 CHANNEL_BRIDGE 时
     * 对客户/坐席两 leg 各调一次（role=user / agent）。
     *
     * <p>⚠️ mod_audio_fork 的 API 语法（audio_fork start &lt;uuid&gt; &lt;ws_url&gt;）待联调确认
     * （按 mdslaney 版 mod_audio_fork；不同版本语法可能略有差异）。
     *
     * @param address  FS 地址
     * @param callId   hsc CallRecord.callId（WS URL query 透传给 ai-callbot）
     * @param legUuid  目标 leg uuid（caller/agent）
     * @param role     user（客户路）/ agent（坐席路）
     * @param agentNumber 坐席号码（WS URL query 透传给 ai-callbot 识别坐席）
     */
    public void forkAudioToAsrBot(String address, String callId, String legUuid, String role, String agentNumber) {
        if (StringUtils.isBlank(legUuid) || StringUtils.isBlank(callId)) {
            return;
        }
        // 拼 WS URL（mod_audio_fork 推流目标，带 callId/role/agentId 供 ai-callbot 识别）
        String wsUrl = bypassAsrBotWs
                + "?callId=" + callId
                + "&role=" + role
                + "&agentId=" + (StringUtils.isBlank(agentNumber) ? "AIBOT" : agentNumber);
        // bgapi uuid_audio_fork <uuid> start <ws_url> <mix-type> <sampling-rate>
        // 参数对齐 mod_audio_fork README API（FS 加载日志确认 API 名=uuid_audio_fork）；
        // mix-type=mono（单方音频，双路各调一次实现说话人分离）；sampling=8k（FS 默认）。
        String args = "uuid_audio_fork " + legUuid + " start " + wsUrl + " mono 8k";
        log.info("旁路 ASR mod_audio_fork: callId={}, role={}, legUuid={}, wsUrl={}",
                callId, role, legUuid, wsUrl);
        sendAsyncMsg(address, "bgapi", args);
    }

    /**
     * 停止旁路 ASR 推流（挂断时对每个 leg 调，可选）。
     */
    public void stopAudioFork(String address, String legUuid) {
        if (StringUtils.isBlank(legUuid)) {
            return;
        }
        sendAsyncMsg(address, "bgapi", "uuid_audio_fork " + legUuid + " stop");
    }

    /**
     * 停止放音
     *
     * @param address
     * @param uniqueId
     */
    public void playBreak(String address, String uniqueId) {
        SendMsg playback = new SendMsg(uniqueId);
        playback.addCallCommand(EslConstant.EXECUTE);
        playback.addExecuteAppName(EslConstant.BREAK_);
        this.sendMsg(address, playback);
    }

    /**
     * 放音文件路径统一翻译：相对路径(如 2026/8/24/xxx.wav、agentbusy.wav)拼上 FS 容器内
     * sound_prefix 绝对路径；流(silence_stream:// 等)与绝对路径原样返回。
     */
    private String toFsPath(String file) {
        if (StringUtils.isBlank(file) || file.contains("://") || file.startsWith("/")) {
            return file;
        }
        return fsSoundsDir + "/" + file;
    }

    /**
     * 播放音乐
     *
     * @param address
     * @param uniqueId
     * @param file
     */
    public void playFile(String address, String uniqueId, String file) {
        SendMsg playfile = new SendMsg(uniqueId);
        playfile.addCallCommand(EslConstant.EXECUTE);
        playfile.addExecuteAppName(EslConstant.PLAYBACK);
        playfile.addExecuteAppArg(toFsPath(file));
        this.sendMsg(address, playfile);
    }

    /**
     * 循环次数播放音乐
     * @param address 地址
     * @param uniqueId 通道ID
     * @param file 文件路径
     * @param number 循环次数
     */
    public void playFile(String address, String uniqueId, String file, Integer number) {
        SendMsg playfile = new SendMsg(uniqueId);
        playfile.addCallCommand(EslConstant.EXECUTE);
        playfile.addExecuteAppName(EslConstant.LOOP_PLAYBACK);
        playfile.addExecuteAppArg("+"+number+ " "+ toFsPath(file));
        this.sendMsg(address, playfile);
    }

    /**
     * 开启ASR
     *
     * @param address
     * @param callId
     * @param uniqueId
     */
    public void detectSpeech(String address, Long callId, String uniqueId) {
        SendMsg speech = new SendMsg(uniqueId);
        speech.addCallCommand(EslConstant.EXECUTE);
        speech.addExecuteAppName(EslConstant.DETECT_SPEECH);
        speech.addExecuteAppArg(EslConstant.UNIMRCP + "mrcpserver alimrcp default");
        this.sendMsg(address, speech);
    }

    public void detectSpeechResume(String address, Long callId, String uniqueId) {
        SendMsg speech = new SendMsg(uniqueId);
        speech.addCallCommand(EslConstant.EXECUTE);
        speech.addExecuteAppName(EslConstant.DETECT_SPEECH);
        speech.addExecuteAppArg(EslConstant.RESUME);
        this.sendMsg(address, speech);
    }


    /**
     * <min> <max> <tries> <timeout> <terminators> <file> <invalid_file> [<var_name> [<regexp> [<digit_timeout> [<transfer_on_failure>]]]]
     * play_and_get_digits(301,1 1 1 2000 q /sounds/welcaome.wav silence_stream://250 SYMWRD_DTMF_RETURN [\*0-9#]+ 2000) execute ok
     * 放音并收号
     * @param adress 媒体地址
     * @param uniqueId 通道ID
     * @param min 最小位数（最小值为 0）
     * @param max 最大位数（最大值为 128）
     * @param tries 声音播放的尝试次数
     * @param timeout 在文件播放结束后和 PAGD 执行重试之前等待拨号响应的毫秒数。
     * @param terminators 终止符 = 如果按下的数字少于 <max>，则用于结束输入的数字。如果它以 '=' 开头，则必须存在终止符才能接受输入（通常为 '#'，可以为空）。在终止数字前面添加 '+' 以始终将其附加到 var_name 中指定的结果变量。
     * @param file 播放声音文件，提示呼叫者拨打数字;播放可以被第一个拨号数字打断（可以为空或特殊字符串 “silence” 以省略消息）。
     * @param invalid_file 当数字与 regexp 不匹配时播放的声音文件（可以为空以省略消息）。
     * @param var_name 应将有效数字放入其中的通道变量（可选，默认情况下不设置任何变量。另请参见下面的“var_name_invalid”）
     * @param regexp 匹配数字的正则表达式（可选，空字符串允许所有输入（默认））。
     * @param digit_timeout 数字间超时;数字之间允许的毫秒数，而不是拨打终止符数字;到达此号码后，PAGD 假定呼叫方没有更多数字可拨打（可选，默认为 <timeout> 的值）。
     * @param transfer_on_failure 达到最大尝试次数时将呼叫转接到何处
     */
    public void playAndGetDigits(String adress, String uniqueId, Integer min, Integer max, Integer tries, Integer timeout, String terminators, String file, String invalid_file, String var_name, String regexp, Integer digit_timeout, String transfer_on_failure){
        StringBuilder builder = new StringBuilder();
        builder.append(min).append(EslConstant.SPACE)
                .append(max).append(EslConstant.SPACE)
                .append(tries).append(EslConstant.SPACE)
                .append(timeout).append(EslConstant.SPACE)
                .append(terminators).append(EslConstant.SPACE)
                .append(toFsPath(file)).append(EslConstant.SPACE)
                .append(toFsPath(invalid_file)).append(EslConstant.SPACE)
                .append(var_name).append(EslConstant.SPACE)
                .append(regexp).append(EslConstant.SPACE);
        if(StringUtils.isNotBlank(transfer_on_failure)){
            builder.append(transfer_on_failure);
        }
        this.sendArgs(adress, uniqueId,EslConstant.PLAY_AND_GET_DIGITS, builder.toString());
    }
}
