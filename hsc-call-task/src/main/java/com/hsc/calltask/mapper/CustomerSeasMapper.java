package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.hsc.calltask.domain.entity.CustomerSeas;

import java.util.List;

/**
 * 客户公海表(CustomerSeas)表数据库访问层
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@Repository()
@Mapper
public interface CustomerSeasMapper extends BaseMapper<CustomerSeas> {

    CustomerSeasVo getDetail(@Param("id") Long id);

    List<CustomerSeasVo> getList(CustomerSeasQuery query);
}

