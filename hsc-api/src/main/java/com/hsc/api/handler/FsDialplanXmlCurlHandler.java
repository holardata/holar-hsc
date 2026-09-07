// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hsc.api.factory.FsXmlCurlEventStrategy;
import com.hsc.common.annotation.XmlCurlEventName;
import com.hsc.common.constant.SectionNames;
import com.hsc.common.xmlcurl.dialplan.Context;
import com.hsc.common.xmlcurl.dialplan.Extension;
import com.hsc.common.domain.FsXmlCurl;
import com.hsc.system.domain.query.dialplan.FsDialplanQuery;
import com.hsc.system.domain.vo.dialplan.FsDialplanVo;
import com.hsc.system.service.IFsDialplanService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * 拨号计划处理类
 *
 * @author danmo
 * @date 2023/09/13 22:02
 **/
@XmlCurlEventName(value = SectionNames.DIALPLAN)
@Slf4j
@Component
public class FsDialplanXmlCurlHandler implements FsXmlCurlEventStrategy {


    @Resource
    private IFsDialplanService iFsDialplanService;

    @Override
    public String eventHandle(FsXmlCurl fsXmlCurl) {
        String xml = "";
        try {
            // 只下发 public 单分区：本产品一切呼叫经 park 进 hsc 统一路由，FS 层拨号计划只是
            // "转运站台"，public/default 分区无区分意义（2026-09-02 决策，详见仓库根
            // 方案/SIP双Profile规范化方案.md 第九节）。不再下发 default，防止库中误配
            // context_name=default 的数据复活"两分区"假象。
            xml += getContext("public");
        } catch (JsonProcessingException e) {
            log.error("FS获取动态拨号计划异常 msg:{}",e.getMessage(),e);
        }
        log.info("dialplanHandle xml curl : {}, {}", JSON.toJSONString(fsXmlCurl, true), xml);
        return xml;
    }

    private String getContext(String name) throws JsonProcessingException {
        Context context = new Context();
        context.setName(name);
        FsDialplanQuery query = new FsDialplanQuery();
        query.setContextName(name);
        List<FsDialplanVo> dialplanList = iFsDialplanService.getList(query);
        if (CollectionUtil.isNotEmpty(dialplanList)) {
            List<Extension> extensionList = new LinkedList<>();
            for (FsDialplanVo fsDialplan : dialplanList) {
                try {
                    Extension extension = new Extension();
                    if ("json".equals(fsDialplan.getType())) {
                        String content = fsDialplan.getContent();
                        extension = JSONObject.parseObject(content, Extension.class);
                    }
                    if ("xml".equals(fsDialplan.getType())) {
                        String content = fsDialplan.getContent();
                        // Extension/Condition/Action 均挂 Jackson XML 注解（与 Context.toXmlString 同源），
                        // 可直接 readValue 绑定，无需 readTree→JSON→fastjson 中转。
                        extension = new XmlMapper().readValue(content, Extension.class);
                    }
                    extension.setName(fsDialplan.getName());
                    extensionList.add(extension);
                } catch (Exception e) {
                    // 单条拨号计划解析失败（type 与 content 不匹配、删分组留下的孤儿脏数据等）
                    // 只跳过该条，不影响同 context 下其他正常拨号计划。否则一条坏数据会让整个
                    // context 返回空，FS 拿不到路由 → 软电话互打 404。
                    log.warn("拨号计划解析失败，已跳过 id={} name={} type={}：{}",
                            fsDialplan.getId(), fsDialplan.getName(), fsDialplan.getType(), e.getMessage());
                }
            }
            context.setExtension(extensionList);
        }
        return context.toXmlString();
    }
}
