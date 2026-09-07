// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.file;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2023-11-02 11:43
 **/
@Schema
@Data
public class VoiceFileVo extends BaseVo {

    /**
     * 主键ID
     */

    @Schema(description = "主键ID")
    private Long id;

    /**
     * 文件名称
     */
    @Schema(description = "文件名称")
    private String name;


    /**
     * 类型 1-手动上传 2-语音合成
     */
    @Schema(description = "类型 1-手动上传 2-语音合成")
    private Integer type;


    /**
     * TTS 引擎 aliyun-阿里云NLS / pro-专业版IndexTTS / holartts-本地HolarTTS / local-本地Qwen-TTS（type=2 生效）
     */
    @Schema(description = "TTS引擎 aliyun/pro/holartts/local(type=2生效)")
    private String tts;

    /**
     * 合成文本
     */
    @Schema(description = "合成文本")
    private String speechText;


    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名称(原文件名，展示用)")
    private String fileName;

    @Schema(description = "文件地址(后端磁盘绝对路径)")
    private String filePath;

    @Schema(description = "文件大小")
    private String fileSize;

    /** cos_id(uuid)，与 file_suffix 拼 uuid 存储名，用于 FS sounds 目录播音(唯一不重名) */
    private String cosId;

    /** 文件后缀(如 mp3/wav) */
    private String fileSuffix;

    /**
     * FS playback 用的 sounds 相对路径：从 filePath(后端磁盘绝对路径 /temp/voice/2026/8/12/uuid.mp3)
     * 截掉挂载源前缀 "/temp/voice/" → "2026/8/12/uuid.mp3"。
     *
     * 原理：后端 /temp/voice 与 FS /usr/local/freeswitch/share/freeswitch/sounds 是同一宿主目录的共享挂载
     * (docker-compose volume)，故 filePath 去掉 "/temp/voice/" 前缀后，就是文件在 FS sounds 目录下的相对路径。
     * 自编译 FS 的 playback 相对路径解析不可依赖(只拼语言目录 en/us/callie 不查 sound_prefix 直拼)，
     * 下发时由 FsClient(playFile/playAndGetDigits) 统一拼成 FS 容器内绝对路径。
     *
     * 用 uuid(cosId)+日期子目录存储，避免同名覆盖且回退值兜底(cosId/suffix 缺失时回退 fileName)。
     */
    public String getUuidName() {
        if (filePath == null || filePath.isEmpty()) {
            return fileName;
        }
        int idx = filePath.indexOf("/temp/voice/");
        if (idx >= 0) {
            return filePath.substring(idx + "/temp/voice/".length());
        }
        // 兜底：filePath 不是预期的 /temp/voice 前缀(老数据/异常路径)，直接用文件名(可能播不了但不报错)
        return fileName;
    }

}
