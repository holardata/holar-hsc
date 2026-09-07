package com.hsc.api.util;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * PDF 文档生成工具(iText 8)。平迁自 reminder_backend PdfUtil。
 *
 * <p>中文字体需外部 ttf 文件(部署时提供 msyh.ttf)，由调用方传入路径。
 */
@Slf4j
public final class PdfUtil {

    private PdfUtil() {
    }

    public static boolean genPdf(List<String> lines, String ttfPath, String docAbPath) {
        try {
            PdfWriter writer = new PdfWriter(docAbPath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont font = PdfFontFactory.createFont(ttfPath, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            if (lines != null && !lines.isEmpty()) {
                for (String line : lines) {
                    document.add(new Paragraph(line).setFont(font));
                }
            } else {
                document.add(new Paragraph("").setFont(font));
            }
            document.close();
            return true;
        } catch (Exception e) {
            log.error("生成 pdf 失败 path={}", docAbPath, e);
            return false;
        }
    }
}
