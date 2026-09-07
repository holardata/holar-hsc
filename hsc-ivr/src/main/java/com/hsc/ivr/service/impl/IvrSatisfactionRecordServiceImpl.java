package com.hsc.ivr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.ivr.domain.entity.IvrSatisfactionRecord;
import com.hsc.ivr.mapper.IvrSatisfactionRecordMapper;
import com.hsc.ivr.service.IIvrSatisfactionRecordService;
import com.hsc.system.domain.vo.dashboard.NameCountVo;
import com.hsc.system.domain.vo.dashboard.SatisfactionVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * IVR满意度评分记录(IvrSatisfactionRecord)表服务实现类
 *
 * @author pangshuai
 * @since 2026-08-17
 */
@Service
public class IvrSatisfactionRecordServiceImpl extends BaseServiceImpl<IvrSatisfactionRecordMapper, IvrSatisfactionRecord> implements IIvrSatisfactionRecordService {

    @Override
    public Map<String, Integer> getLatestScoreByCallIds(Collection<String> callIds) {
        Map<String, Integer> scoreMap = new HashMap<>();
        if (callIds == null || callIds.isEmpty()) {
            return scoreMap;
        }
        List<IvrSatisfactionRecord> records = list(new LambdaQueryWrapper<IvrSatisfactionRecord>()
                .in(IvrSatisfactionRecord::getCallId, callIds)
                .orderByDesc(IvrSatisfactionRecord::getId));
        // 按 id 倒序遍历，同 callId 首条即最新一条
        for (IvrSatisfactionRecord record : records) {
            scoreMap.putIfAbsent(record.getCallId().toString(), record.getScore());
        }
        return scoreMap;
    }

    @Override
    public SatisfactionVo getSatisfaction(String begin, String end) {
        SatisfactionVo vo = new SatisfactionVo();
        List<NameCountVo> distribution = new ArrayList<>();
        long totalScore = 0;
        long totalCount = 0;
        List<Map<String, Object>> rows = baseMapper.statByScore(begin, end);
        for (int star = 1; star <= 5; star++) {
            long count = 0;
            for (Map<String, Object> row : rows) {
                Object scoreObj = row.get("score");
                int score = Objects.isNull(scoreObj) ? 0 : ((Number) scoreObj).intValue();
                if (score == star) {
                    Object cntObj = row.get("cnt");
                    count = Objects.isNull(cntObj) ? 0 : ((Number) cntObj).longValue();
                    break;
                }
            }
            distribution.add(new NameCountVo(star + "星", count));
            totalScore += star * count;
            totalCount += count;
        }
        vo.setTotal(totalCount);
        vo.setDistribution(distribution);
        if (totalCount > 0) {
            vo.setAvgScore(new BigDecimal(totalScore).divide(new BigDecimal(totalCount), 1, RoundingMode.HALF_UP));
        }
        return vo;
    }
}
