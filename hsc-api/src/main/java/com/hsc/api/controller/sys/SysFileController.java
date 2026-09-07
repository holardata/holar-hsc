// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.sys;

import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.domain.file.FileUploadVo;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.SysFile;
import com.hsc.system.service.ISysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/system/v1/file")
public class SysFileController extends BaseController {

    @Autowired
    private ISysFileService sysFileService;

    /**
     * 文件上传
     *
     * @param file 文件
     * @param type 上传方式 local ali  tx
     * @return 文件上传结果
     */
    @Operation(description = "文件上传", method = "POST")
    @PostMapping("/upload")
    public ResResult<FileUploadVo> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadVo uploadedFile = sysFileService.uploadFile(file,null);
        return success(uploadedFile);
    }

    /**
     * 文件播放(流)：按 fileId 读磁盘文件原样输出，供前端 audio 试听。
     * filePath 是后端磁盘绝对路径(见 LocalFileUploadHandler)，浏览器无法直接访问，故走此接口代理。
     * 已在 SecurityConfig 放行(permitAll)，前端 audio src 直连即可(语音文件非敏感、原生 audio 不便带 token)。
     */
    @Operation(description = "文件播放(流)", method = "GET")
    @GetMapping("/play/{id}")
    public void playFile(@PathVariable("id") Long id, HttpServletResponse response) {
        SysFile sysFile = sysFileService.getById(id);
        if (sysFile == null || sysFile.getFilePath() == null) {
            throw new CommonException("文件不存在");
        }
        File file = new File(sysFile.getFilePath());
        if (!file.isFile()) {
            throw new CommonException("文件不存在或已删除");
        }
        // 按 suffix 推断 MIME，默认给通用二进制流
        String suffix = sysFile.getFileSuffix();
        response.setContentType(inferContentType(suffix));
        response.setContentLengthLong(file.length());
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
            out.flush();
        } catch (IOException e) {
            // 客户端中断等：不抛 500，仅日志(client aborted 常见于 audio 中途停止)
            log.warn("播放文件流出错 fileId={} err={}", id, e.getMessage());
        }
    }

    /**
     * 按后缀推断 Content-Type。除音频试听外，OEM 品牌图片（logo/favicon/登录页大图，
     * svg/ico/png 等）也走本接口免鉴权访问——svg 按 octet-stream 输出会被浏览器
     * 安全策略拒绝渲染，须给出正确 MIME。
     */
    private String inferContentType(String suffix) {
        if (suffix == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return switch (suffix.toLowerCase()) {
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "ico" -> "image/x-icon";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }


}
