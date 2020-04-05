package pt.unl.fct.di.apdc.firstwebapp.util;

public enum userRoles {
	USER, GBO, GA, GS, SU;
	
	public static boolean contains(String test) {
		for (userRoles c : userRoles.values()) {
			if (c.name().equalsIgnoreCase(test)) {
				return true;
			}
		}
		return false;
	}
}
