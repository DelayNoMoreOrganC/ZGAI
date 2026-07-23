package com.lawfirm.dto;

import lombok.Data;
import java.util.List;

@Data
public class KnowledgeImportConfirmRequest {
    private List<Long> itemIds;
}
