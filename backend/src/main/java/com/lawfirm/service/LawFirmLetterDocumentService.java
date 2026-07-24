package com.lawfirm.service;

import com.lawfirm.entity.LawFirmLetter;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDate;

@Service
public class LawFirmLetterDocumentService {
    private static final String FONT = "仿宋_GB2312";

    public byte[] generate(LawFirmLetter letter) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePage(document);
            XWPFParagraph title = paragraph(document, ParagraphAlignment.CENTER, 36, true, null);
            addRun(title, "广东至高律师事务所函", 36, true, false);
            paragraph(document, ParagraphAlignment.LEFT, 14, true, null);

            XWPFParagraph number = paragraph(document, ParagraphAlignment.RIGHT, 16, true, null);
            addRun(number, displayNumber(letter), 16, true, false);
            paragraph(document, ParagraphAlignment.RIGHT, 16, true, null);

            XWPFParagraph recipient = paragraph(document, ParagraphAlignment.LEFT, 16, true, null);
            addRun(recipient, letter.getRecipient(), 16, true, true);
            addRun(recipient, "：", 16, true, false);

            XWPFParagraph body = paragraph(document, ParagraphAlignment.LEFT, 16, false, 200);
            addRun(body, "我所接受", 16, false, false);
            addRun(body, letter.getClientName(), 16, true, true);
            addRun(body, "的委托，指派", 16, false, false);
            addRun(body, letter.getLawyerNames(), 16, true, true);
            addRun(body, "担任其与", 16, false, false);
            addRun(body, letter.getOpposingParty(), 16, true, true);
            addRun(body, letter.getCaseReason(), 16, false, false);
            addRun(body, "一案的代理人。", 16, false, false);

            XWPFParagraph spacer = paragraph(document, ParagraphAlignment.LEFT, 16, false, null);
            setExactLineSpacing(spacer, 460);
            addRun(spacer, "   ", 16, false, false);

            XWPFParagraph closing = paragraph(document, ParagraphAlignment.LEFT, 16, false, 200);
            addRun(closing, letter.getClosingText(), 16, false, false);
            paragraph(document, ParagraphAlignment.LEFT, 16, false, null);

            XWPFParagraph firm = paragraph(document, ParagraphAlignment.RIGHT, 16, true, null);
            addRun(firm, "广东至高律师事务所", 16, true, false);
            XWPFParagraph date = paragraph(document, ParagraphAlignment.RIGHT, 16, true, null);
            addRun(date, chineseDate(letter.getIssueDate()), 16, true, false);

            paragraph(document, ParagraphAlignment.LEFT, 14, true, null);
            paragraph(document, ParagraphAlignment.LEFT, 14, false, null);
            footer(document, "联系电话：0757—83283000    传真：0757—83905969", 500);
            footer(document, "代理律师电话：" + letter.getLawyerContacts() + "    邮编：528000", 480);
            footer(document, "地址：佛山市禅城区岭南大道北100号岭南大厦16层", 500);
            footer(document, "", 500);

            document.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成律所所函失败", e);
        }
    }

    public String displayNumber(LawFirmLetter letter) {
        if (letter.getLetterNumber() != null && !letter.getLetterNumber().trim().isEmpty()) {
            return letter.getLetterNumber();
        }
        int year = letter.getIssueDate() == null ? LocalDate.now().getYear() : letter.getIssueDate().getYear();
        return String.format("(%d)粤至高%s函字第【待编号】号", year, letter.getLetterTypeCode());
    }

    private void configurePage(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz size = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        size.setW(BigInteger.valueOf(11906));
        size.setH(BigInteger.valueOf(16838));
        CTPageMar margins = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1916));
        margins.setRight(BigInteger.valueOf(1797));
        margins.setBottom(BigInteger.valueOf(1134));
        margins.setLeft(BigInteger.valueOf(1916));
    }

    private XWPFParagraph paragraph(XWPFDocument document, ParagraphAlignment alignment,
                                    int size, boolean bold, Integer firstLineChars) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        if (firstLineChars != null) {
            CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
            CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
            ind.setFirstLineChars(BigInteger.valueOf(firstLineChars));
        }
        XWPFRun defaultRun = paragraph.createRun();
        styleRun(defaultRun, size, bold, false);
        defaultRun.setText("");
        return paragraph;
    }

    private void addRun(XWPFParagraph paragraph, String text, int size, boolean bold, boolean underline) {
        XWPFRun run = paragraph.createRun();
        styleRun(run, size, bold, underline);
        run.setText(text == null ? "" : text);
    }

    private void styleRun(XWPFRun run, int size, boolean bold, boolean underline) {
        run.setFontFamily(FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setUnderline(underline ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
        run.setColor("000000");
        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = properties.sizeOfRFontsArray() > 0 ? properties.getRFontsArray(0) : properties.addNewRFonts();
        fonts.setAscii(FONT);
        fonts.setHAnsi(FONT);
        fonts.setEastAsia(FONT);
        fonts.setCs(FONT);
    }

    private void footer(XWPFDocument document, String text, int spacing) {
        XWPFParagraph paragraph = paragraph(document, ParagraphAlignment.LEFT, 14, true, null);
        setExactLineSpacing(paragraph, spacing);
        addRun(paragraph, text, 14, true, false);
    }

    private void setExactLineSpacing(XWPFParagraph paragraph, int line) {
        CTPPr pPr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(line));
        spacing.setLineRule(STLineSpacingRule.EXACT);
    }

    private String chineseDate(LocalDate date) {
        LocalDate value = date == null ? LocalDate.now() : date;
        String digits = "〇一二三四五六七八九";
        StringBuilder year = new StringBuilder();
        for (char digit : String.valueOf(value.getYear()).toCharArray()) {
            year.append(digits.charAt(digit - '0'));
        }
        return year + "年" + chineseNumber(value.getMonthValue()) + "月" + chineseNumber(value.getDayOfMonth()) + "日";
    }

    private String chineseNumber(int value) {
        String[] numbers = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        if (value <= 10) return numbers[value];
        if (value < 20) return "十" + numbers[value - 10];
        int ones = value % 10;
        return numbers[value / 10] + "十" + (ones == 0 ? "" : numbers[ones]);
    }
}
