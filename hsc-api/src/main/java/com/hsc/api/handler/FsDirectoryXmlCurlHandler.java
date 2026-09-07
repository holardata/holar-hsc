// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.handler;

import com.alibaba.fastjson.JSONObject;
import com.hsc.api.factory.FsXmlCurlEventStrategy;
import com.hsc.common.annotation.XmlCurlEventName;
import com.hsc.common.constant.SectionNames;
import com.hsc.common.domain.FsXmlCurl;
import com.hsc.common.xmlcurl.sofia.domain.DirectoryDomain;
import com.hsc.common.xmlcurl.group.Group;
import com.hsc.common.xmlcurl.group.Groups;
import com.hsc.common.xmlcurl.param.ParamEnum;
import com.hsc.common.xmlcurl.param.Params;
import com.hsc.common.xmlcurl.sofia.Param;
import com.hsc.common.xmlcurl.user.Users;
import com.hsc.common.xmlcurl.user.User;
import com.hsc.common.xmlcurl.variables.Variable;
import com.hsc.common.xmlcurl.variables.VariableEnum;
import com.hsc.common.xmlcurl.variables.Variables;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.subsriber.KoSubscriberQuery;
import com.hsc.system.domain.vo.sip.KoSubscriberVo;
import com.hsc.system.service.IKoSubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * sip用户动态管理（不需要kamailio注册用户的使用）
 * @author: danmo
 * @date: 2025/09/09
 */

@Slf4j
@Service
@RequiredArgsConstructor
@XmlCurlEventName(SectionNames.DIRECTORY)
public class FsDirectoryXmlCurlHandler implements FsXmlCurlEventStrategy {

    private final IKoSubscriberService koSubscriberService;

    @Override
    public String eventHandle(FsXmlCurl fsXmlCurl) {
        log.info("directory xml curl req json : [{}]", JSONObject.toJSONString(fsXmlCurl));
        String xml = "";
        try {
            xml = this.getDomain(fsXmlCurl);
        } catch (Exception e) {
            log.error("Directory 配置获取异常 msg:{}", e.getMessage(), e);
        }
        log.info("FsDirectoryXmlCurlHandler: {}", xml);
        return xml;
    }

    private String getDomain(FsXmlCurl xmlCurl) throws Exception {
        List<User> users = new ArrayList<>();
        String user = xmlCurl.getMetadata().get("user");
        if (user == null) {
            KoSubscriberQuery query = new KoSubscriberQuery();
            query.setStatus(0);
            //按自己的sip账号逻辑获取，这里仅演示
            List<KoSubscriberVo> sipUsers = koSubscriberService.getList(query);
            if (CollectionUtils.isNotEmpty(sipUsers)) {
                for (KoSubscriberVo sipUser : sipUsers) {
                    User xmlUser = new User(sipUser.getUserName(), null,
                            new Params(Collections.singletonList(new Param(ParamEnum.PASSWORD.key, sipUser.getPassword()))),
                            new Variables(Arrays.asList(new Variable(VariableEnum.USER_CONTEXT.key, "default"), new Variable(VariableEnum.EFFECTIVE_CALLER_ID_NUMBER.key, sipUser.getUserName()))),
                            null);
                    users.add(xmlUser);
                }
            }
        }else {
            KoSubscriber sipUser = koSubscriberService.getByUserName(user);
            if (Objects.isNull(sipUser)) {
                log.warn("Directory 查无此SIP用户 user:{} ip:{}，返回空配置，注册将被FreeSWITCH拒绝", user, xmlCurl.getMetadata().get("ip"));
                return "";
            }
            User xmlUser = new User(sipUser.getUsername(), null,
                    new Params(Collections.singletonList(new Param(ParamEnum.PASSWORD.key, sipUser.getPassword()))),
                    new Variables(Arrays.asList(new Variable(VariableEnum.USER_CONTEXT.key, "default"), new Variable(VariableEnum.EFFECTIVE_CALLER_ID_NUMBER.key, sipUser.getUsername()))),
                    null);
            users.add(xmlUser);
        }

        DirectoryDomain domain = new DirectoryDomain();
        domain.setName("$${domain}")
                .setParams(new Params().setParam(Arrays.asList(
                        new Param(ParamEnum.DIAL_STRING.key, "{^^:sip_invite_domain=${dialed_domain}:presence_id=${dialed_user}@${dialed_domain}}${sofia_contact(*/${dialed_user}@${dialed_domain})},${verto_contact(${dialed_user}@${dialed_domain})}"),
                        new Param(ParamEnum.JSONRPC_ALLOWED_METHODS.key, "verto")
                ))).setVariables(new Variables(Arrays.asList(
                        new Variable(VariableEnum.DEFAULT_GATEWAY.key, "$${default_provider}"),
                        new Variable(VariableEnum.DEFAULT_AREACODE.key, "$${default_areacode}"),
                        new Variable(VariableEnum.TRANSFER_FALLBACK_EXTENSION.key, "operator")
                ))).setGroups(new Groups(Collections.singletonList(new Group(
                                new Users(users)
                        )
                )
                ))
        ;

        return domain.toXmlString();
    }


}
