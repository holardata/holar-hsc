package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.query.CustomerTemplateQuery;
import com.hsc.calltask.domain.vo.CustomerTemplateVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.hsc.calltask.domain.entity.CustomerTemplate;

import java.util.List;

/**
 * 客户模板管理表(CustomerTemplate)表数据库访问层
 *
 * @author danmo
 * @since 2025-06-30 11:35:44
 */
@Repository()
@Mapper
public interface CustomerTemplateMapper extends BaseMapper<CustomerTemplate> {

    CustomerTemplateVo getDetail(@Param("id") Long id);

    List<Long> getIdsByQuery(CustomerTemplateQuery query);

    List<CustomerTemplateVo> getList(CustomerTemplateQuery query);
}

