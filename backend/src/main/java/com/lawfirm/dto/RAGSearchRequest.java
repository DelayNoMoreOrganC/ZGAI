package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;

/**
 * RAG检索请求DTO
 */
public class RAGSearchRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer topK = 5; // 返回最相关的K篇文档

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
