package pt.unl.fct.di.apdc.firstwebapp.util;

public enum AccStates {
	ACTIVE, INACTIVE;

	public static boolean contains(String test) {
		for (AccStates c : AccStates.values()) {
			if (c.name().equalsIgnoreCase(test)) {
				return true;
			}
		}
		return false;
	}
}

