package com.lawfirm.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseSearchKeywordTest {

    @Test
    void tokenizesMultipleSearchTerms() {
        assertEquals(List.of("华润银行", "006-1"), CaseService.tokenizeCaseSearch("  华润银行   006-1  "));
    }

    @Test
    void normalizesSpacesAndCommonPunctuationInCaseNames() {
        assertEquals("佛山农商银行金融借款纠纷",
                CaseService.normalizeSearchText("佛山 农商银行（金融借款纠纷）"));
    }

    @Test
    void normalizesFormalAndAbbreviatedCaseNumbersToComparableText() {
        assertEquals("2026民006103",
                CaseService.normalizeCaseNumberSearchText("[2026]粤至高民字第006-103号"));
        assertEquals("2026民006",
                CaseService.normalizeCaseNumberSearchText("2026民006"));
    }
}
