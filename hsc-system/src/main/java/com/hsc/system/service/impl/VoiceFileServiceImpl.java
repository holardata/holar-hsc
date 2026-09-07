// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.domain.file.FileUploadVo;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.SysFile;
import com.hsc.system.domain.entity.VoiceFile;
import com.hsc.system.domain.query.file.VoiceFileAddQuery;
import com.hsc.system.domain.query.file.VoiceFileQuery;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.mapper.VoiceFileMapper;
import com.hsc.system.service.IAiCallbotConfigService;
import com.hsc.system.service.ISysFileService;
import com.hsc.system.service.IVoiceFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 语音文件表(VoiceFile)表服务实现类
 *
 * @author danmo
 * @since 2023-11-01 14:35:02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VoiceFileServiceImpl extends BaseServiceImpl<VoiceFileMapper, VoiceFile> implements IVoiceFileService {

    private final IAiCallbotConfigService aiCallbotConfigService;
    private final ISysFileService iSysFileService;

    @Override
    public void add(VoiceFileAddQuery query) {
        checkName(query.getName());
        VoiceFile voiceFile = new VoiceFile();
        voiceFile.setQuery2Entity(query);
        // type=1 手动上传：前端 uploadFile 时已落盘到 /temp/voice/...(与 FS 共享挂载，FS playback 天然能读)，直接落库
        // type=2 TTS：先合成并上传成功拿到 fileId 再落库——失败在落库前抛错，不留无音频的脏记录（试听列为空且重试撞"名称已存在"）
        if(Objects.nonNull(query.getType()) && query.getType() == 2){
            voiceFile.setFileId(synthesizeAndReplaceFile(voiceFile, query.getTts(), query.getSpeechText()).getId());
        }
        save(voiceFile);
    }

    @Override
    public void edit(VoiceFileAddQuery query) {
        VoiceFile voiceFile = getById(query.getId());
        if(Objects.isNull(voiceFile)){
            throw new CommonException("无效ID");
        }
        VoiceFile updateVoiceFile = new VoiceFile();
        if(!StringUtils.equals(voiceFile.getName(), query.getName())){
            checkName(query.getName());
        }
        updateVoiceFile.setQuery2Entity(query);
        // 引擎或文本任一变化都需重新合成（换引擎试听是常见操作）；
        // 先合成并上传成功拿到新 fileId 再落库——失败在落库前抛错，避免记录已改为新文本/新引擎但试听仍是旧音频
        if(Objects.nonNull(query.getType()) && query.getType() == 2){
            boolean textChanged = !StringUtils.equals(voiceFile.getSpeechText(), query.getSpeechText());
            boolean engineChanged = !Objects.equals(voiceFile.getTts(), query.getTts());
            if(textChanged || engineChanged){
                updateVoiceFile.setFileId(synthesizeAndReplaceFile(voiceFile, query.getTts(), query.getSpeechText()).getId());
            }
        }
        updateById(updateVoiceFile);
    }

    @Override
    public void regenerate(VoiceFileAddQuery query) {
        if(!Objects.equals(query.getType(), 2)){
            throw new CommonException("仅语音合成类型的语音文件支持重新生成");
        }
        VoiceFile voiceFile = getById(query.getId());
        if(Objects.isNull(voiceFile)){
            throw new CommonException("无效ID");
        }
        if(!StringUtils.equals(voiceFile.getName(), query.getName())){
            checkName(query.getName());
        }
        VoiceFile updateVoiceFile = new VoiceFile();
        updateVoiceFile.setQuery2Entity(query);
        // 无条件强制重合（音频偶发问题时原样重生成），旧音频文件在合成成功后清理（见 synthesizeAndReplaceFile）
        updateVoiceFile.setFileId(synthesizeAndReplaceFile(voiceFile, query.getTts(), query.getSpeechText()).getId());
        updateById(updateVoiceFile);
    }

    @Override
    public void delete(VoiceFileQuery query) {
        List<Long> ids = new LinkedList<>();
        if (Objects.nonNull(query.getId())) {
            ids.add(query.getId());
        }
        if (CollectionUtil.isNotEmpty(query.getIds())) {
            ids.addAll(query.getIds());
        }
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        // 删除前先查出关联的 SysFile，用于清理文件记录与本地物理文件
        List<VoiceFile> voiceFiles = listByIds(ids);
        for (VoiceFile voiceFile : voiceFiles) {
            Long fileId = voiceFile.getFileId();
            if (fileId == null) {
                continue;
            }
            SysFile sysFile = iSysFileService.getById(fileId);
            if (sysFile != null) {
                // 删物理文件：filePath 是后端 /temp/voice/... 的绝对路径，与 FS sounds 是同一宿主文件(共享挂载)，
                // 删一次两边都没了。容错：文件不存在/删除失败仅日志，不阻断 DB 删除。
                deleteLocalFile(sysFile.getFilePath());
                iSysFileService.removeById(fileId);
            }
        }
        List<VoiceFile> list = ids.stream().map(id -> {
            VoiceFile voiceFile = new VoiceFile();
            voiceFile.setId(id);
            voiceFile.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return voiceFile;
        }).collect(Collectors.toList());
        updateBatchById(list);
    }

    /**
     * TTS 合成 + 上传新音频，并清理记录旧音频文件（SysFile 记录 + 物理文件）。
     * 新增 / 编辑重合 / 手动重新生成共用。清理时机在合成 + 上传成功之后——失败时旧音频
     * 保留、记录不落库，不留空窗。返回新文件的 SysFile 信息（含新 fileId）。
     */
    private FileUploadVo synthesizeAndReplaceFile(VoiceFile record, String tts, String speechText) {
        File ttsFile = aiCallbotConfigService.synthesizeToFile(parseEngineId(tts), speechText);
        try {
            FileUploadVo fileUploadVo = iSysFileService.uploadFile(ttsFile, null);
            Long oldFileId = record.getFileId();
            if (oldFileId != null) {
                SysFile oldSysFile = iSysFileService.getById(oldFileId);
                if (oldSysFile != null) {
                    // filePath 是后端 /temp/voice/... 绝对路径，与 FS sounds 同一宿主文件(共享挂载)，删一次两边都没
                    deleteLocalFile(oldSysFile.getFilePath());
                    iSysFileService.removeById(oldFileId);
                }
            }
            return fileUploadVo;
        } finally {
            // uploadFile 是复制到 /temp/voice，临时合成文件用完即删，避免 /tmp 堆积
            deleteLocalFile(ttsFile.getAbsolutePath());
        }
    }

    /**
     * 删除本地物理文件，容错：文件不存在/删除失败仅记日志，不阻断 DB 删除
     * （filePath 可能是 URL/远程路径/已不存在的临时文件，删除失败不应影响业务删除）。
     */
    private void deleteLocalFile(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return;
        }
        try {
            File file = new File(filePath);
            if (file.isFile()) {
                FileUtil.del(file);
            }
        } catch (Exception e) {
            log.warn("删除语音文件物理文件失败 filePath={}, err={}", filePath, e.getMessage());
        }
    }

    @Override
    public VoiceFileVo getDetail(Long id) {
        return this.baseMapper.getDetail(id);
    }

    @Override
    public List<VoiceFileVo> getPageList(VoiceFileQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return getList(query);
    }

    @Override
    public List<VoiceFileVo> getList(VoiceFileQuery query) {
        return this.baseMapper.getList(query);
    }

    private void checkName(String name){
        long count = count(new LambdaQueryWrapper<VoiceFile>().eq(VoiceFile::getName, name).eq(VoiceFile::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (count > 0){
            throw new CommonException("名称已存在！");
        }
    }

    /**
     * voice_file.tts 存语音引擎实例 id（unify-voice-engine-config）。
     * 兼容兜底：脏数据（'pro' 等历史旧枚举）转 Long 失败时给可读错误提示重选引擎。
     */
    private Long parseEngineId(String tts) {
        if (StringUtils.isBlank(tts)) {
            throw new CommonException("请选择 TTS 引擎实例");
        }
        try {
            return Long.valueOf(tts);
        } catch (NumberFormatException e) {
            throw new CommonException("TTS 引擎实例无效，请重新选择语音引擎实例");
        }
    }
}

