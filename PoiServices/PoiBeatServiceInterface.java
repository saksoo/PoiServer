package PoiServices;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

  
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style=Style.DOCUMENT)
public interface PoiBeatServiceInterface 
{
	// 1.	public String registerUser(String username#password1#password2)
	public String registerUser(String username_password1_password2);
		
	// 2.	public String setMonitorData(String username#password, String newEntry)
	public String setMonitorData(String username_password, String newEntry);
	
	// 3.	public String getMapData(String username#password, String position)
	public String getMapData(String username_password, String position);

	// 4.   login...
	public boolean loginUser(String username_password);
}