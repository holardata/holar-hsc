package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallSkill;
import com.hsc.system.domain.query.skill.CallSkillAddQuery;
import com.hsc.system.domain.query.skill.CallSkillQuery;
import com.hsc.system.domain.vo.skill.CallSkillVo;

import java.util.List;

/**
 * 技能表(CallSkill)表服务接口
 *
 * @author danmo
 * @since 2024-10-29 14:21:52
 */
public interface ICallSkillService extends IBaseService<CallSkill> {

    void add(CallSkillAddQuery query);

    void edit(CallSkillAddQuery query);

    void delete(CallSkillQuery query);

    CallSkillVo getDetail(Long id);

    PageInfo<CallSkillVo> pageList(CallSkillQuery query);

    List<CallSkillVo> getList(CallSkillQuery query);

    List<CallSkillVo> getListByIds(CallSkillQuery query);
}

