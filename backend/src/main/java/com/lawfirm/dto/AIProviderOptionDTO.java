package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AIProviderOptionDTO {
    private String providerType;
    private String displayName;
    private String modelName;
    private boolean available;
    private boolean local;
    private String privacyNotice;
}
