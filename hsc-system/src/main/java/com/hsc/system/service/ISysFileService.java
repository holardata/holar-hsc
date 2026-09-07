package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.common.domain.file.FileUploadVo;
import com.hsc.system.domain.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件管理(SysFile)表服务接口
 *
 * @author danmo
 * @since 2024-11-18 10:22:33
 */
public interface ISysFileService extends IBaseService<SysFile> {

    /**
     * 文件上传
     *
     * @param file 文件
     * @param type  上传方式 local ali  tx
     * @return 文件信息
     */
    FileUploadVo uploadFile(MultipartFile file, String type);
    /**
     * 文件上传
     *
     * @param file 文件
     * @param type  上传方式 local ali  tx
     * @return 文件信息
     */
    FileUploadVo uploadFile(File file, String type);


}

