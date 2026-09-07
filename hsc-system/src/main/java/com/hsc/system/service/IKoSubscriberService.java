// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.subsriber.KoSubscriberAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberBatchAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberUpdateQuery;
import com.hsc.system.domain.vo.sip.KoSubscriberVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;

import java.util.List;

/**
 * (Subscriber)
 *
 * @author danmo
 * @date 2024-07-29 10:49:24
 */
public interface IKoSubscriberService extends IBaseService<KoSubscriber> {

    void add(KoSubscriberAddQuery query);

    void batchAdd(KoSubscriberBatchAddQuery query);

    void edit(KoSubscriberUpdateQuery query);

    KoSubscriber getDetail(Integer id);

    void delete(KoSubscriberQuery query);

    List<KoSubscriberVo> getList(KoSubscriberQuery query);

    KoSubscriber getByUserName(String username);


    List<KoSubscriberVo> getPageList(KoSubscriberQuery query);

    List<SipSimpleVo> selectList();

    /** 启用号码下拉,可按终端类型过滤(terminalType=null 不过滤) */
    List<SipSimpleVo> selectList(Integer terminalType);

}
