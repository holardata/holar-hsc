package com.hsc.ivr.service;

import com.hsc.common.base.IBaseService;
import com.hsc.ivr.domain.entity.IvrSatisfactionRecord;
import com.hsc.system.domain.vo.dashboard.SatisfactionVo;

import java.util.Collection;
import java.util.Map;

/**
 * IVR满意度评分记录(IvrSatisfactionRecord)表服务接口
 *
 * @author pangshuai
 * @since 2026-08-17
 */
public interface IIvrSatisfactionRecordService extends IBaseService<IvrSatisfactionRecord> {

    /**
     * 批量查每个通话的最新满意度评分（话单列表回填用）。
     *
     * @param callIds 通话ID集合
     * @return callId → 最新一条评分(0-未评价)；无记录的 callId 不在 map 中
     */
    Map<String, Integer> getLatestScoreByCallIds(Collection<String> callIds);

    /**
     * 满意度统计（dashboard satisfaction：score>0 计入，均分+1-5 星分布；空数据返回空结构）
     */
    SatisfactionVo getSatisfaction(String begin, String end);
}
