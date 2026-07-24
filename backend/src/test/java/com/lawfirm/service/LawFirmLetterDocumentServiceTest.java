package com.lawfirm.service;

import com.lawfirm.entity.LawFirmLetter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LawFirmLetterDocumentServiceTest {
    private final LawFirmLetterDocumentService service = new LawFirmLetterDocumentService();

    @Test
    void generatedDocumentKeepsTemplateStyleAndUsesBlackText() throws Exception {
        LawFirmLetter letter = sampleLetter();
        byte[] content = service.generate(letter);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("广东至高律师事务所函");
            XWPFRun title = document.getParagraphs().get(0).getRuns().stream()
                    .filter(run -> !run.text().isEmpty()).findFirst().orElseThrow();
            assertThat(title.getFontSize()).isEqualTo(36);
            assertThat(title.isBold()).isTrue();
            assertThat(document.getParagraphs().stream().map(item -> item.getText()).reduce("", String::concat))
                    .contains("(2026)粤至高民函字第【待编号】号", "佛山市某人民法院", "广东至高律师事务所");
            assertThat(document.getParagraphs().stream().flatMap(item -> item.getRuns().stream())
                    .map(XWPFRun::getColor).filter(value -> value != null && !value.isEmpty()))
                    .containsOnly("000000");
        }
    }

    @Test
    void approvedLetterUsesAssignedNumber() {
        LawFirmLetter letter = sampleLetter();
        letter.setLetterNumber("(2026)粤至高民函字第25号");
        assertThat(service.displayNumber(letter)).isEqualTo("(2026)粤至高民函字第25号");
    }

    private LawFirmLetter sampleLetter() {
        LawFirmLetter letter = new LawFirmLetter();
        letter.setRecipient("佛山市某人民法院");
        letter.setClientName("委托人甲");
        letter.setLawyerNames("张三律师、李四律师");
        letter.setOpposingParty("相对方乙");
        letter.setCaseReason("合同纠纷");
        letter.setLetterTypeCode("民");
        letter.setLawyerContacts("张三：13800000000");
        letter.setClosingText("特此函告！");
        letter.setIssueDate(LocalDate.of(2026, 7, 24));
        return letter;
    }
}
