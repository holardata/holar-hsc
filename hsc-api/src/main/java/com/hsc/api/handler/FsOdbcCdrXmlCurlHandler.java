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
import com.hsc.common.xmlcurl.odbccdr.OdbcCdrConfiguration;
import com.hsc.system.domain.entity.FsModules;
import com.hsc.common.domain.FsXmlCurl;
import com.hsc.system.domain.query.modules.FsModulesQuery;
import com.hsc.system.service.IFsModulesService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * odbc_cdr 处理类【当前为死代码，留观未删（2026-08-27 排查定论，与 FsXmlCdrXmlCurlHandler 同批）】
 *
 * <p>它是什么：FS「自产话单」的另一路线（mod_odbc_cdr 模块）的配置下发口。与 xml_cdr 路线
 * （FsXmlCdrXmlCurlHandler）目的相同——让 FS 自己产话单，区别是送达方式：mod_odbc_cdr 不走 HTTP、
 * 不经后端，FS 通过 ODBC 直连数据库把话单写进指定表。若 FS 加载了该模块，启动时会经 xml_curl
 * 回调请求 CONFIGURATION:ODBC_CDR.CONF 分发到本 handler，从 fs_modules 表（前端「线路配置→
 * 模块配置」页，已停用）读出配置（settings=连接参数 DSN/用户名/密码，tables=写哪张表哪些字段，
 * 结构见 OdbcCdrConfiguration）拼成 odbc_cdr.conf 的 XML 下发给 FS。
 *
 * <p>为什么当前没用（与 xml_cdr 同因，且断得更彻底）：
 * 1. FS 镜像未编译 mod_odbc_cdr：Dockerfile.2-build 只启用了 mod_xml_curl；且它额外依赖
 *    unixODBC，Dockerfile.1-deps 也没装；
 * 2. modules.conf.xml 无加载行；
 * 3. 模块不加载 → FS 永不发起该 configuration 请求 → 本 handler 永不会被触发；
 * 数据源 fs_modules「模块配置」页同是死功能，2026-08 已停用。
 *
 * <p>为什么留着不删：本系统话单走 ESL 事件链路（CHANNEL_HANGUP_COMPLETE → call_record 表，
 * 即「通话记录」页），此路线纯冗余。理论上它的适用场景是"话单量极大、不想承担每通一个 HTTP
 * 回调的开销、让 FS 直写库"，但当前单机规模远用不上。CDR 旁路与本系统 ESL 话单方案的关系
 * （孰为"规范"、计费视角 vs 业务视角、各自定位与短板）详见 FsXmlCdrXmlCurlHandler 类注释，此处不重复。
 *
 * <p>要启用的完整步骤（若真要启用话单旁路，优先评估 xml_cdr 路线——不用装 ODBC 驱动、
 * 后端有现成接收落库，本路线还要自己建目标话单表）：
 * 1. Dockerfile.1-deps 补 unixODBC/unixODBC-devel；Dockerfile.2-build 参照 mod_xml_curl 的
 *    sed 写法启用 mod_odbc_cdr 编译；
 * 2. modules.conf.xml 加 load mod_odbc_cdr；
 * 3. 重建 FS 镜像并部署；
 * 4. fs_modules 补一条：name='odbc_cdr.conf'、type='json'，content 为 settings（dsn/username/
 *    password 等连接参数）+ tables（表名与字段列表，FS 按字段配置把 channel variable 直插表），
 *    结构对齐 OdbcCdrConfiguration；fs_modules 无预置数据，需手工造；
 * 5. 目标话单表自行建表（FS 按配置直插，与后端实体无关）。
 *
 * @author danmo
 * @date 2023/09/13 22:02
 **/
@XmlCurlEventName(value = SectionNames.ODBC_CDR)
@Slf4j
@Component
public class FsOdbcCdrXmlCurlHandler implements FsXmlCurlEventStrategy {

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
        OdbcCdrConfiguration configuration = getConfiguration(name);
        return configuration.toXmlString();
    }



    private OdbcCdrConfiguration getConfiguration(String name) throws JsonProcessingException {
        OdbcCdrConfiguration cdrConfiguration = new OdbcCdrConfiguration();
        FsModulesQuery query = new FsModulesQuery();
        query.setName(name);
        List<FsModules> fsModules = iFsModulesService.getList(query);
        if(CollectionUtil.isNotEmpty(fsModules)){
            FsModules fsModule = fsModules.get(0);
            if ("json".equals(fsModule.getType())) {
                String content = fsModule.getContent();
                cdrConfiguration = JSONObject.parseObject(content, OdbcCdrConfiguration.class);
            }
            if ("xml".equals(fsModule.getType())) {
                String content = fsModule.getContent();
                String valueAsString = new ObjectMapper().writeValueAsString(new XmlMapper().readTree(content));
                cdrConfiguration = JSONObject.parseObject(valueAsString, OdbcCdrConfiguration.class);
            }
        }

        return cdrConfiguration;
    }


}
