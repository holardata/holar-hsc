package com.hsc.esl.service;

import com.hsc.system.domain.vo.sip.FsRegVo;

import java.util.List;

/**
 * FreeSWITCH SIP 注册查询(`sofia status profile internal reg`)。
 *
 * <p>下沉至 hsc-esl：FsAgentRouteHandler(hsc-esl) 需据此判断坐席分机是否注册到 FS，
 * 决定是否 originate；hsc-api 的 KoSubscriberController 经 hsc-api→hsc-esl 依赖同样可用。
 */
public interface ISipRegService {

    /**
     * 查 FS 注册列表;username 为空查全部,非空按账号过滤。
     */
    List<FsRegVo> getList(String username);
}
