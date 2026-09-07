package com.hsc.api.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件压缩工具(JDK java.util.zip)。平迁自 reminder_backend ZipUtil，改 try-with-resources 修复句柄泄漏。
 */
@Slf4j
public final class ZipUtil {

    private ZipUtil() {
    }

    public static void zipFiles(List<String> srcFiles, String zipFileName) {
        byte[] buffer = new byte[1024];
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String srcFile : srcFiles) {
                File fileToZip = new File(srcFile);
                try (FileInputStream fis = new FileInputStream(fileToZip)) {
                    zos.putNextEntry(new ZipEntry(fileToZip.getName()));
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
            log.info("zip 生成完成 {}", zipFileName);
        } catch (IOException e) {
            log.error("zip 压缩失败 {}", zipFileName, e);
        }
    }
}
