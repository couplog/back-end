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

	public static String generateConnectionCode(int length) {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int startNumber;
			int endNumber;
			if (secureRandom.nextBoolean()) {
				startNumber = 0;
				endNumber = 10;
				res.append(secureRandom.nextInt(startNumber, endNumber));
			} else {
				startNumber = 0;
				endNumber = 'Z' - 'A' + 1;
				res.append((char) (secureRandom.nextInt(startNumber, endNumber) + 'A'));
			}
		}
		return res.toString();
	}

}
