// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.file.service;

import com.hsc.common.domain.file.FileUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author danmo
 * @date 2023-11-01 15:15
 **/
public interface IFileUploadService {

    /**
     * @param file 文件
     * @param type 上传方式 local ali  tx
     * @return
     */
    FileUploadVo fileUpload(MultipartFile file, String type);


    FileUploadVo fileUpload(File file, String type);
}
