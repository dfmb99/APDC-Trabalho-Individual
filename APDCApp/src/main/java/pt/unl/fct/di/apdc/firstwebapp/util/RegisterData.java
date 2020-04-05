package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

	public String username;
	public String email;	
	public String password;
	public String confirmation;
	public String profile;
	public String landlinePhone;
	public String mobilePhone;
	public String street;
	public String accState;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String email, String password, String confirmation, String profile, String landlinePhone, String mobilePhone, String street, String accState) {
			this.username = username;
			this.email = email;
			this.password = password;
			this.confirmation = confirmation;
			this.profile = profile;
			this.landlinePhone = landlinePhone;
			this.mobilePhone = mobilePhone;
			this.street = street;
			this.accState = accState;
	}
	
	private boolean validField(String value) {
		return value != null && !value.equals("");
	}
	
	public boolean validRegistration() {
		return validField(username) &&
			   validField(email) &&
			   validField(password) &&
			   validField(confirmation) &&
			   password.equals(confirmation) &&
			   email.contains("@") &&
			   AccProfile.contains(profile) && 
			   AccStates.contains(accState);		
	}

}
