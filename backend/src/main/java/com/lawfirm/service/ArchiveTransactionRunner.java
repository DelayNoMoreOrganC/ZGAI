package com.lawfirm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ArchiveTransactionRunner {
    private final PlatformTransactionManager transactionManager;

    public <T> T execute(Supplier<T> callback) {
        return new TransactionTemplate(transactionManager).execute(status -> callback.get());
    }
}
