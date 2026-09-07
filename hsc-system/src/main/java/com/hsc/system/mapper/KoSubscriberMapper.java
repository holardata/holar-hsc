package com.hsc.system.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.subsriber.KoSubscriberQuery;
import com.hsc.system.domain.vo.sip.KoSubscriberVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (Subscriber)
 *
 * @author danmo
 * @date 2023-08-29 10:49:24
 */
@Mapper
public interface KoSubscriberMapper extends BaseMapper<KoSubscriber> {


    KoSubscriber getByUserName(@Param("username") String username);


    List<KoSubscriberVo> getList(KoSubscriberQuery query);

    List<KoSubscriber> getByUserNameList(@Param("userNameList") List<String> userNameList);
}

