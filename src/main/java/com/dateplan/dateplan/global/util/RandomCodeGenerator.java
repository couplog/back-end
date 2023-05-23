package com.dateplan.dateplan.global.util;

import java.security.SecureRandom;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomCodeGenerator {

	private static final SecureRandom secureRandom = new SecureRandom();

	public static int generateCode(int length) {

		int startNumber = (int) Math.pow(10, length - 1);
		int endNumber = (int) Math.pow(10, length) - 1;

		return secureRandom.nextInt(startNumber, endNumber);
	}
}
