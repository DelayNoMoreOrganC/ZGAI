package com.lawfirm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OcrHealthDTO {
    private String status;
    private boolean textDocumentExtractionReady;
    private boolean imageOcrReady;
    private boolean scannedPdfOcrReady;
    private String language;
    private String message;
}
