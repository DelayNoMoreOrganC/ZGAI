package com.lawfirm.converter;

import com.lawfirm.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA字段加密转换器
 * 用于自动加密敏感字段
 */
@Converter
@Component
@RequiredArgsConstructor
public class EncryptConverter implements AttributeConverter<String, String> {

    private final CryptoUtil cryptoUtil;

    /**
     * 保存到数据库前加密
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        return cryptoUtil.encrypt(attribute);
    }

    /**
     * 从数据库读取后解密
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        return cryptoUtil.decrypt(dbData);
    }
}
