package com.hsc.api.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Word(docx) 文档生成工具(POI XWPF)。平迁自 reminder_backend DocxUtil。
 */
@Slf4j
public final class DocxUtil {

    private DocxUtil() {
    }

    public static boolean genDocx(List<String> lines, String docAbPath) {
        try (XWPFDocument document = new XWPFDocument()) {
            if (lines != null && !lines.isEmpty()) {
                for (String line : lines) {
                    document.createParagraph().createRun().setText(line);
                }
            } else {
                document.createParagraph().createRun().setText("");
            }
            try (FileOutputStream out = new FileOutputStream(docAbPath)) {
                document.write(out);
            }
            return true;
        } catch (Exception e) {
            log.error("生成 docx 失败 path={}", docAbPath, e);
            return false;
        }
    }
}
