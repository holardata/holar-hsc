package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.query.route.CallRouteQuery;
import com.hsc.system.domain.vo.route.CallRouteVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.hsc.system.domain.entity.CallRoute;

import java.util.List;

/**
 * 号码路由表(CallRoute)表数据库访问层
 *
 * @author danmo
 * @since 2024-12-30 14:03:40
 */
@Repository()
@Mapper
public interface CallRouteMapper extends BaseMapper<CallRoute> {

    CallRouteVo getDetail(@Param("id") Long id);

    List<CallRouteVo> getList(CallRouteQuery query);
}

