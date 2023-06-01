package com.dateplan.dateplan.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Converter
public class PasswordConverter implements AttributeConverter<String, String> {

	private final PasswordEncryptor passwordEncryptor;

	@Override
	public String convertToDatabaseColumn(String rawPassword) {
		return passwordEncryptor.encryptPassword(rawPassword);
	}

	@Override
	public String convertToEntityAttribute(String encryptedPassword) {
		return encryptedPassword;
	}
}
