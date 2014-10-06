package PoiServices;


import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(endpointInterface="PoiServices.PoiBeatServiceInterface") 
public class PoiBeatServiceImplementation implements PoiBeatServiceInterface {
	private final Application application;

	public PoiBeatServiceImplementation(Application application) {
		this.application = application;
		System.out.println("PoiBeatService implementation initialized. ");
	}

	@Override
    @WebMethod
	public String registerUser(String username_password1_password2) {
		if (username_password1_password2 == null) {
			return "";
		}
		return application.registerUser(username_password1_password2);
	}

	@Override
	@WebMethod
	public String setMonitorData(String username_password, String newEntry) {
		if (username_password == null || newEntry == null ) {
			return "";
		}
		return application.setMonitorData(username_password, newEntry);
	}

	@Override
	@WebMethod
	public String getMapData(String username_password, String position) {
		if (username_password == null || position == null ) {
			return "";
		}
		return application.getMapData(username_password, position);
	}
	
	@Override
	@WebMethod
	public boolean loginUser(String username_password){
		return application.loginUser(username_password);
	}
}
