// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hsc.api.factory.FsXmlCurlEventStrategy;
import com.hsc.common.annotation.XmlCurlEventName;
import com.hsc.common.constant.SectionNames;
import com.hsc.common.xmlcurl.sofia.setting.Settings;
import com.hsc.common.xmlcurl.xmlcdr.XmlCdrConfiguration;
import com.hsc.system.domain.entity.FsModules;
import com.hsc.common.domain.FsXmlCurl;
import com.hsc.system.domain.query.modules.FsModulesQuery;
import com.hsc.system.service.IFsModulesService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xml_cdr 处理类【当前为死代码，留观未删（2026-08-27 排查定论，与 FsOdbcCdrXmlCurlHandler 同批）】
 *
 * <p>它是什么：FS「自产话单」方案（mod_xml_cdr 模块）的配置下发口。若 FS 加载了 mod_xml_cdr，
 * 模块启动时会经 xml_curl 回调请求 CONFIGURATION:XML_CDR.CONF 分发到本 handler，从 fs_modules 表
 * （前端「线路配置→模块配置」页，已停用）读出配置（话单 POST 地址/编码等 param，结构见
 * XmlCdrConfiguration.settings）拼成 xml_cdr.conf 的 XML 下发给 FS。之后每通电话挂断，FS 会把话单
 * HTTP POST 到 FsXmlCdrController（POST /fs/cdr/api，已在免鉴权白名单）→ FsXmlCdrServiceImpl
 * 解析落 fs_cdr 表——下发口（本类）+ 接收口（FsXmlCdrController）整条链路代码是现成的。
 *
 * <p>为什么当前没用（FS 侧三层全断，本 handler 永不会被触发）：
 * 1. FS 镜像未编译 mod_xml_cdr：deploy/freeswitch/Dockerfile.2-build 编译时只启用了 mod_xml_curl；
 * 2. modules.conf.xml 未加载：mod_xml_cdr 的 load 行被注释（模块二进制不存在，放开注释也没用）；
 * 3. 模块不加载 → FS 永不发起该 configuration 请求 → FsXmlCurlEventStrategyFactory 匹配不到，
 *    本 handler 无人调用；且数据源「模块配置」页（fs_modules）本身是死功能，2026-08 已停用。
 *
 * <p>与本系统 ESL 话单方案的关系——"CDR 旁路才是规范做法"是误解，两者是计费视角 vs 业务视角：
 * <ul>
 * <li>CDR（Call Detail Record，呼叫明细记录）出身是电信计费传统：运营商结算、企业对账、按分钟收费，
 *     要求"挂断后交付一份字段标准、不可抵赖、绝不丢失的话单"。mod_xml_cdr 是这个传统的产物，
 *     特点：①事后交付——挂断后才 POST，通话进行中什么都拿不到；②电信视角——字段全在信令/媒体层
 *     （billsec 计费时长、progress/answer/bridge 时间戳、挂断原因、编解码），不知道也不关心业务层。</li>
 * <li>ESL 事件驱动（mod_event_socket）同样是 FS 官方一等公民接口，设计目的就是"让外部程序当 FS 的
 *     大脑"；呼叫中心平台（FusionPBX 及大量商业呼叫中心）普遍走 ESL 事件流做实时话单/弹屏/监控。
 *     本系统呼叫控制整个建立在 ESL 上——park 拦截、8 种路由分发、AI 转人工编排、坐席状态、旁路 ASR
 *     触发全是事件驱动，话单只是这条事件流的副产品，顺着既有架构走是顺理成章而非将就。</li>
 * <li>业务字段 CDR 给不了（这正是本系统选 ESL 的原因）：
 *     坐席归属/接听方式、AI 先接听(is_ai_first)/转人工时间(transfer_time)、录音文件、转写/摘要关联
 *     （靠后端雪花 callId 从 PARK 起贯穿全部腿；CDR 只有 FS 通道 uuid，多腿合并还得自己做映射）、
 *     通话中实时更新话单——这些 call_record 都有、fs_cdr 都没有；反向 billsec/bridge_stamp 等电信
 *     字段则是 fs_cdr 天生全（本系统当初 ESL handler 没写的就没有）。真换成 CDR 方案，转写/录音/
 *     摘要/坐席归属全挂不上号，最后还得在 ESL 链路维护业务状态、两套并存，CDR 那套成纯摆设。
 *     佐证：FsXmlCdrServiceImpl 里就解析了 billsec/bridge_stamp/sip_hangup_disposition——上游原始
 *     设计就是"call_record 管业务、fs_cdr 管电信"两路互补，从来不是拿一路替代另一路。</li>
 * <li>ESL 方案唯一真实短板（诚实记录）：话单可靠性 = ESL 长连的可靠性。后端与 FS 断线窗口内的事件
 *     丢了就丢了，话单可能残缺且无 FS 侧兜底；mod_xml_cdr 相反——POST 失败会落盘（err-dir 机制），
 *     后端恢复后可补投，话单不丢。因此当话单要用于计费/审计/对账（"一分钱都不能差"）时，行业标准
 *     确实会要求一条不依赖业务系统存活的话单兜底通道——那才是启用本旁路的真正时机；当前用途
 *     （通话记录查询/业务统计）下 ESL 方案是更合适的那条路。类比：mod_xml_cdr 是银行流水（事后/
 *     标准/绝不错漏/对账用），ESL 话单是记账 App（实时/带业务标签）——用途不同，不存在谁不规范。</li>
 * </ul>
 *
 * <p>为什么留着不删：如上，当前业务下 ESL 方案更合适、本旁路冗余；但将来出现计费/对账/话单外推需求时，
 * 启用本链路（下发口+接收口+fs_cdr 表）代码是现成的：
 * · 与运营商对账：FS 原生话单字段（billsec/progress_stamp/bridge_stamp/sip_hangup_disposition 等）
 *   比 call_record 更细更"电信级"；
 * · 把话单旁路推给外部系统：mod_xml_cdr 的 HTTP POST 天然适合，改 url 指向对方即可（但那样
 *   后端下发口只需配 url，接收口仍收自己这份）。
 *
 * <p>要启用的完整步骤：
 * 1. deploy/freeswitch/Dockerfile.2-build：参照 mod_xml_curl 的 sed 写法启用 mod_xml_cdr 编译；
 * 2. deploy/freeswitch/conf/autoload_configs/modules.conf.xml：放开 mod_xml_cdr 的 load 行；
 * 3. 重建 FS 镜像并部署（build.sh，只需阶段 2/3）；
 * 4. fs_modules 表补一条：name='xml_cdr.conf'、type='json'、content 为 settings 的 param 列表，
 *    如 {"param":[{"name":"url","value":"http://后端:4320/fs/cdr/api"},{"name":"encode","value":"text"}]}；
 * 5. 重启 FS：模块加载时回调 xml_curl 拿本 handler 下发的配置，之后挂断即 POST 话单落 fs_cdr 表
 *    （FsXmlCdrServiceImpl 按 callId 幂等 saveOrUpdate）。
 * ⚠️ 注意两点：① fs_modules 无预置数据（system.sql 没插），第 4 步需手工造；② FsXmlCdrServiceImpl
 * 落库依赖 Redis 的 callInfo 能按 uuid 对上 callId（CALL_REL_MAP_CACHE_KEY 映射，ESL 链路在
 * CHANNEL_CREATE/PARK 时写入），对不上会静默丢弃——启用前先在测试环境验证全链路。
 *
 *
 * “FS 把话单 POST 出来”是什么场景的规范
 *
 *   CDR（Call Detail Record，呼叫明细记录）这个词出身就是电信计费：运营商之间结算、企业跟运营商对账、按分钟收费——需要的是“挂断后交付一份字段标准、不可抵赖、绝
 *   不丢失的话单”。mod_xml_cdr 就是这个传统的产物，它的设计目标是给计费系统/对账系统喂数据，特点是：
 *
 *   - 事后交付：挂断后才 POST，通话进行中什么都拿不到
 *   - 电信视角：字段全是信令/媒体层（billsec 计费时长、progress/answer/bridge 时间戳、挂断原因、编解码）——它不知道也不关心业务层发生了什么
 *
 *   ESL 事件驱动同样是官方规范路线
 *
 *   ESL（mod_event_socket）是 FreeSWITCH 官方的一等公民接口，设计目的就是“让外部程序当 FS 的大脑”。呼叫中心平台（包括 FusionPBX 及大量商业呼叫中心）普遍走 ESL
 *   事件流做实时话单、弹屏、监控。你的系统整个呼叫控制本来就建立在 ESL 上——park 拦截、8 种路由分发、AI 转人工编排、坐席状态、旁路 ASR
 *   触发全是事件驱动。话单只是这条事件流的副产品，顺着已有的架构走，这不是将就，是顺理成章。
 *
 *   更关键的是你的业务字段 CDR 给不了：
 *
 *   ┌───────────────────────────────────┬──────────────────────────────┬──────────────────────────────────────────┐
 *   │         你需要的话单信息          │   ESL 方案（call_record）    │          mod_xml_cdr（fs_cdr）           │
 *   ├───────────────────────────────────┼──────────────────────────────┼──────────────────────────────────────────┤
 *   │ 哪个坐席接的、接听方式            │ ✅ 实时写入                  │ ❌ 无此概念                              │
 *   ├───────────────────────────────────┼──────────────────────────────┼──────────────────────────────────────────┤
 *   │ AI 先接听、转人工时间点           │ ✅ PARK 时就标 is_ai_first   │ ❌ 无此概念                              │
 *   ├───────────────────────────────────┼──────────────────────────────┼──────────────────────────────────────────┤
 *   │ 录音文件、转写、摘要关联          │ ✅ 同一雪花 callId 贯穿      │ ❌ 只有 FS 通道 uuid，多腿还得自己做映射 │
 *   ├───────────────────────────────────┼──────────────────────────────┼──────────────────────────────────────────┤
 *   │ 实时性（通话中更新话单）          │ ✅ 事件即到即写              │ ❌ 挂断后才有一份                        │
 *   ├───────────────────────────────────┼──────────────────────────────┼──────────────────────────────────────────┤
 *   │ billsec / bridge_stamp 等电信字段 │ ⚠️ 当初 handler 没写的就没有 │ ✅ 天生全                                │
 *   └───────────────────────────────────┴──────────────────────────────┴──────────────────────────────────────────┘
 *
 *   真换成 CDR 方案，你会发现转写、录音、摘要、坐席归属全挂不上号，最后还得在 ESL 链路上维护业务状态——两套并存，CDR 那套变成纯摆设。所以这两种方案不是“规范 vs
 *   不规范”，是业务视角 vs 计费视角，你的系统要的是前者。
 *
 *   一个佐证：FsXmlCdrServiceImpl 里其实解析了 billsec、bridge_stamp、sip_hangup_disposition 这些电信字段——上游的原始设计就是“两路互补”（call_record
 *   管业务、fs_cdr 管电信），从来不是拿一路替代另一路。
 *
 *   你的方案真实存在的短板（诚实说）
 *
 *   话单可靠性 = ESL 长连的可靠性。 后端与 FS 断线窗口内的事件丢了就丢了，话单可能残缺，而 FS 侧没有任何兜底重发；mod_xml_cdr 相反——POST 失败会落盘（err-dir
 *   机制），后端恢复后可补投，话单不丢。
 *
 *   这就是“规范”二字里唯一站得住的质疑：如果话单要用于计费、审计、跟运营商对账，行业标准确实会要求有一条不依赖业务系统存活的话单兜底通道。这正是我在类注释里写
 *   的“与运营商对账时启用 mod_xml_cdr”的真正含义——不是你现在做错了，而是哪天话单有了“一分钱都不能差”的用途，再把那条旁路打开做对账兜底。你现在的用途（通话记录
 *   查询、业务统计）下，ESL 方案是更合适的那条路。
 *
 *   类比收尾：mod_xml_cdr 是银行流水（事后、标准、绝不错漏、对账用），你的 ESL 话单是记账
 *   App（实时、带分类标签、业务视角）。没人会说“银行流水才是规范记账、记账 App 不规范”——用途不同罢了。
 *
 *
 * @author danmo
 * @date 2023/09/13 22:02
 **/
@XmlCurlEventName(value = SectionNames.XML_CDR)
@Slf4j
@Component
public class FsXmlCdrXmlCurlHandler implements FsXmlCurlEventStrategy {

    @Resource
    private IFsModulesService iFsModulesService;

    @Override
    public String eventHandle(FsXmlCurl fsXmlCurl) {
        String xml = "";
        try {
            xml += getContext(fsXmlCurl.getKeyValue());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        log.info("dialplanHandle xml curl : {}, {}", JSON.toJSONString(fsXmlCurl, true), xml);
        return xml;
    }

    private String getContext(String name) throws JsonProcessingException {
        XmlCdrConfiguration xmlCdrConfiguration = new XmlCdrConfiguration();
        xmlCdrConfiguration.setName(name);
        xmlCdrConfiguration.setDescription("XML CDR CURL logger");
        xmlCdrConfiguration.setSettings(getSettParam(name));
        return xmlCdrConfiguration.toXmlString();
    }

    private Settings getSettParam(String name) throws JsonProcessingException {
        Settings settings = new Settings();
        FsModulesQuery query = new FsModulesQuery();
        query.setName(name);
        List<FsModules> fsModules = iFsModulesService.getList(query);
        if (CollectionUtil.isNotEmpty(fsModules)) {
            FsModules fsModule = fsModules.get(0);
            if ("json".equals(fsModule.getType())) {
                String content = fsModule.getContent();
                settings = JSONObject.parseObject(content, Settings.class);
            }
            if ("xml".equals(fsModule.getType())) {
                String content = fsModule.getContent();
                String param = new ObjectMapper().writeValueAsString(new XmlMapper().readTree(content));
                settings = JSONObject.parseObject(param, Settings.class);
            }
        }
        return settings;
    }
}
