package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.query.CustomerFieldQuery;
import com.hsc.calltask.domain.vo.CustomerFieldVo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.hsc.calltask.domain.entity.CustomerField;

import java.util.List;

/**
 * 客户字段管理表(CustomerField)表数据库访问层
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@Repository()
@Mapper
public interface CustomerFieldMapper extends BaseMapper<CustomerField> {

    List<CustomerFieldVo> getList(CustomerFieldQuery query);
}

