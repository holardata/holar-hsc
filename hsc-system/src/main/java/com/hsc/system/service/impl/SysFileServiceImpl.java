// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.constant.SysSettingConfig;
import com.hsc.common.domain.file.FileUploadVo;
import com.hsc.common.exception.FileException;
import com.hsc.file.service.IFileUploadService;
import com.hsc.system.domain.entity.SysFile;
import com.hsc.system.mapper.SysFileMapper;
import com.hsc.system.service.ISysFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件管理(SysFile)表服务实现类
 *
 * @author danmo
 * @since 2024-11-18 10:22:33
 */
@Service
public class SysFileServiceImpl extends BaseServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    @Autowired
    private IFileUploadService fileUploadService;

    @Autowired
    private SysSettingConfig lfsSettingConfig;

    @Override
    public FileUploadVo uploadFile(MultipartFile file, String type) {
        FileUploadVo fileUploadVo;
        try {
            fileUploadVo = fileUploadService.fileUpload(file, type);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new FileException("文件上传失败");
        }
        SysFile sysFile = new SysFile();
        sysFile.setFilePath(fileUploadVo.getFilePath());
        sysFile.setCosId(fileUploadVo.getCosId());
        sysFile.setFileName(fileUploadVo.getFileName());
        sysFile.setFileSize(fileUploadVo.getFileSize());
        sysFile.setFileSuffix(fileUploadVo.getFileSuffix());
        sysFile.setFileType(fileUploadVo.getFileType());
        save(sysFile);
        fileUploadVo.setId(sysFile.getId());
        return fileUploadVo;
    }

    @Override
    public FileUploadVo uploadFile(File file, String type) {
        FileUploadVo fileUploadVo;
        try {
            fileUploadVo = fileUploadService.fileUpload(file, type);
        } catch (Exception e) {
            throw new FileException("文件上传失败");
        }
        SysFile sysFile = new SysFile();
        sysFile.setFilePath(fileUploadVo.getFilePath());
        sysFile.setCosId(fileUploadVo.getCosId());
        sysFile.setFileName(fileUploadVo.getFileName());
        sysFile.setFileSize(fileUploadVo.getFileSize());
        sysFile.setFileSuffix(fileUploadVo.getFileSuffix());
        sysFile.setFileType(fileUploadVo.getFileType());
        save(sysFile);
        fileUploadVo.setId(sysFile.getId());
        return fileUploadVo;
    }
}

