package com.kalpesh.bankstatement.model;

import java.util.Arrays;

public enum Mode {

	DEBIT_CARD("DEBIT CARD"), NET_BANKING("NET BANKING"), ICICI_ATM("ICICI ATM");

	private final String mode;

	Mode(String mode) {
		this.mode = mode;
	}

	public static boolean contains(String mode) {
		for (Mode mode1 : Mode.values()) {
			if (mode1.mode.equals(mode)) {
				return true;
			}
		}
		return false;
	}

	public static Mode value(String mode) {
		return Arrays.stream(Mode.values()).filter(mode1 -> mode1.mode.equals(mode)).findFirst().get();
	}
}
