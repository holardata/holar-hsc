// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.VoiceFile;
import com.hsc.system.domain.query.file.VoiceFileAddQuery;
import com.hsc.system.domain.query.file.VoiceFileQuery;
import com.hsc.system.domain.vo.file.VoiceFileVo;

import java.util.List;

/**
 * 语音文件表(VoiceFile)表服务接口
 *
 * @author danmo
 * @since 2023-11-01 14:35:02
 */
public interface IVoiceFileService extends IBaseService<VoiceFile> {

    void add(VoiceFileAddQuery query);

    void edit(VoiceFileAddQuery query);

    /**
     * 重新生成：删除旧音频文件（SysFile 记录 + 物理文件）并按表单值强制重新合成。
     * 仅 type=2 语音合成类型可用；合成成功后才落库并清理旧文件。
     */
    void regenerate(VoiceFileAddQuery query);

    void delete(VoiceFileQuery query);

    VoiceFileVo getDetail(Long id);

    List<VoiceFileVo> getPageList(VoiceFileQuery query);

    List<VoiceFileVo> getList(VoiceFileQuery query);

    /**
     * 语音文件 id → FS 播放名（sounds 相对路径，FsClient.playFile 下发时再拼绝对路径）。
     * 统一入口：技能组排队音/播报音/转坐席音等播放名解析都走这里，勿在调用方内联查库翻译。
     *
     * @param voiceId 语音文件ID
     * @return 未传 id / 文件不存在返回 null，由调用方自行兜底
     */
    default String getPlayName(Long voiceId) {
        if (voiceId == null) {
            return null;
        }
        VoiceFileVo detail = getDetail(voiceId);
        return detail == null ? null : detail.getUuidName();
    }
}

