package pt.unl.fct.di.apdc.firstwebapp.util;

public enum AccProfile {
	PUBLIC, PRIVATE;
	
	public static boolean contains(String test) {
		for (AccProfile c : AccProfile.values()) {
			if (c.name().equalsIgnoreCase(test)) {
				return true;
			}
		}
		return false;
	}
}
