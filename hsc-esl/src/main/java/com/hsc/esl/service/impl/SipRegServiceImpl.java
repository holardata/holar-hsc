package com.hsc.esl.service.impl;

import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.vo.sip.FsRegVo;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询 FS 的 SIP 注册列表(`sofia status profile internal reg`)。
 *
 * <p>FS 输出为"每字段一行"的键值对格式(Call-ID/User/Contact/Status/Auth-User/IP/Port 各占一行,
 * 每条注册之间空行分隔)。按空行分块、块内首个冒号切 key/value 解析;username 取 Auth-User(分机号),
 * 不能用 Contact 里的随机 user——WebRTC 软电话的 Contact 是 JsSIP 生成的伪地址(host.invalid,无端口)。
 */
@Slf4j
@Service
public class SipRegServiceImpl implements ISipRegService {

    /**
     * FsClient 经 @Lazy 延迟注入以打破循环依赖：本类与 FsClient 同处 hsc-esl 模块，
     * FsClient→事件服务→事件工厂→...→FsAgentRouteHandler→本类→FsClient 成环；
     * 原构造器注入(final)无法被 allow-circular-references 解决，改字段注入 + @Lazy。
     * （本类由 5fd98f2 从别处移入 hsc-esl 时引入此环）
     */
    @Lazy
    @Autowired
    private FsClient fsClient;

    /** Status 行里的剩余秒数,形如 EXPSECS(655) */
    private static final Pattern EXP_SECS = Pattern.compile("EXPSECS\\((\\d+)\\)");

    @Override
    public List<FsRegVo> getList(String username) {
        Optional<String> address = fsClient.getFirstFsAddress();
        if (address.isEmpty()) {
            log.warn("SIP注册查询:未配置FS主机(fs_config 为空)");
            return Collections.emptyList();
        }
        EslMessage msg = fsClient.sendSyncMsg(address.get(), "sofia", "status profile internal reg");
        if (msg == null) {
            log.warn("SIP注册查询:FS 未连接或无响应 address={}", address.get());
            return Collections.emptyList();
        }
        // sofia 原始输出降 debug：本查询被技能组 fsAcd 每 2s 调一次，INFO 级全量 dump(含分机/Contact/IP)
        // 会刷屏且泄露注册信息；排查 parse 时开 debug 日志对照
        if (log.isDebugEnabled()) {
            log.debug("SIP注册查询 address={}, sofia 原始输出=\n[{}]",
                    address.get(), String.join("]\n[", msg.getBodyLines()));
        }
        List<FsRegVo> all = parse(msg.getBodyLines());
        log.debug("SIP注册查询:解析出 {} 条注册", all.size());
        if (username == null || username.isBlank()) {
            return all;
        }
        return all.stream().filter(r -> username.equals(r.getUsername())).toList();
    }

    /**
     * 解析 sofia 注册输出。按空行分块(每块一条注册),块内以首个冒号切 key/value。
     * 分隔线(等号行)/标题行无冒号,自动跳过。
     */
    private List<FsRegVo> parse(List<String> bodyLines) {
        List<FsRegVo> list = new ArrayList<>();
        if (bodyLines == null) {
            return list;
        }
        Map<String, String> kv = new HashMap<>();
        for (String raw : bodyLines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                addReg(kv, list);
                kv.clear();
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            kv.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        // 收尾:末块后可能无空行
        addReg(kv, list);
        return list;
    }

    /** 一条注册块转为 FsRegVo;无 Auth-User/User 的块(标题/分隔/Total 行)跳过。 */
    private void addReg(Map<String, String> kv, List<FsRegVo> list) {
        if (kv.isEmpty()) {
            return;
        }
        String username = kv.get("Auth-User");
        if ((username == null || username.isEmpty()) && kv.get("User") != null) {
            username = kv.get("User").split("@", 2)[0];
        }
        if (username == null || username.isEmpty()) {
            return;
        }
        FsRegVo vo = new FsRegVo();
        vo.setUsername(username);
        vo.setCallId(kv.get("Call-ID"));
        vo.setContact(kv.get("Contact"));
        vo.setIp(kv.get("IP"));
        vo.setPort(kv.get("Port"));
        vo.setUserAgent(kv.get("Agent"));
        vo.setStatus(kv.get("Status"));
        vo.setExpires(extractExpSecs(kv.get("Status")));
        list.add(vo);
    }

    /** 从 Status 行提取 EXPSECS(n) 的剩余秒数。 */
    private String extractExpSecs(String status) {
        if (status == null) {
            return null;
        }
        Matcher m = EXP_SECS.matcher(status);
        return m.find() ? m.group(1) : null;
    }
}
