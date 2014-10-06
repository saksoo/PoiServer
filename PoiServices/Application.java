package PoiServices;


import java.util.Properties;

import javax.xml.ws.Endpoint; 

import Configuration.ControlPanelInformation;
import Configuration.DatabaseInformation;
import Configuration.WebServiceInformation;

public class Application {
	public final DatabaseInformation databaseInformation;
	private final WebServiceInformation webserviceInformation;
	private final ControlPanelInformation cpInformation;
	private final ControlPanel cp;
	public final Database database;
	private final PoiBeatServiceImplementation poiBeatService = new PoiBeatServiceImplementation(this);
	private Endpoint endpoint;
	
	public Application(Properties p) {
		databaseInformation = new DatabaseInformation(p);	//boithitikes klaseis
		webserviceInformation = new WebServiceInformation(p);
		cpInformation = new ControlPanelInformation(p);
		cp = new ControlPanel(cpInformation, this);		//new gui
		
		System.out.println(databaseInformation);	// for debug
		System.out.println(webserviceInformation);
		System.out.println(cpInformation);
		
		database = new Database(databaseInformation); 	// connect to database
		database.addObserver(cp);
		
		System.out.println("Application initialized successfully.");
	}
	
	public void start() {
		try {
			endpoint = Endpoint.publish("http://" + webserviceInformation.ip + ":" +  webserviceInformation.port +"/PoiBeat/", poiBeatService);
			// http://hostname:10000/PoiBeat/
			//http://192.168.1.68:10000/PoiBeat/PoiBeat/?wsdl
			System.out.println("Service published successfully: " + System.getenv("COMPUTERNAME"));
			
			cp.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error while publishing the web service: " + e.getMessage());
		}
	}
	
	public void stop() {
		try {
			endpoint.stop();	
			System.out.println("Endpoint closed.");
			database.close();
			System.out.println("Database closed.");
			
			cp.setVisible(false); 									//close window 
			cp.dispose();
		} catch (Exception e) {
			System.err.println("Error while closing server: " + e.getMessage());
		}
	}
	
	public String registerUser(String buffer) {
		if (buffer.split("#").length != 3) {
			return "Fail: Incorrect number of parameters";
		}
		String[] user_details = buffer.split("#");
		String u = user_details[0];
		String p1 = user_details[1];
		String p2 = user_details[2];
		if (!p1.equals(p2)) {
			return "Fail: Passwords do not match";
		}
		if (u.length() < 3) {
			return "Fail: username should have at least 3 letters";
		}
		int i = database.registerUser(u, p1);
		if (i==0) {
			return "Fail: username duplicate";
		} else {
			return "OK";
		}
	}

	public String setMonitorData(String buffer, String newEntry) {
		if (buffer.split("#").length != 2 || newEntry.split("#").length != 3) {
			return "Fail: Incorrect number of parameters";
		}
		String[] user_details = buffer.split("#");
		String u = user_details[0];
		String p = user_details[1];
		
		String[] poi_details = newEntry.split("#");
		String location = poi_details[0];
		String type = poi_details[1];
		String name = poi_details[2];
		int x = Integer.parseInt(location.split(",")[0]);
		int y = Integer.parseInt(location.split(",")[1]);
		
		if (database.authenticateUser(u, p)) {
			database.setMonitorData(u, x, y, type, name, databaseInformation.R);
			return "OK";
		} else {
			System.out.println("User authentication failed.");
			return "Fail: AUTH";
		}
	}

	public String getMapData(String buffer, String position) {
		if (buffer.split("#").length != 2 || position.split(",").length != 3) {
			return "Fail: Incorrect number of parameters";
		}
		String[] user_details = buffer.split("#");
		String u = user_details[0];
		String p = user_details[1];
		
		String[] position_details = position.split(",");			// x,y,R
		int x = Integer.parseInt(position_details[0]);
		int y = Integer.parseInt(position_details[1]);
		int R = Integer.parseInt(position_details[2]);
		
		if (database.authenticateUser(u, p)) {
			String result = database.getMapData(u, x, y, R);
			return result;
		} else {
			System.out.println("User authentication failed.");
		}
		
		return "FAIL";
	}
	
	public boolean loginUser(String buffer) {
		if (buffer.split("#").length != 2) {
			return false;
		}
		String[] user_details = buffer.split("#");
		String u = user_details[0];
		String p = user_details[1];
		if (u.length() < 3) {
			return false;
		}
		boolean b = database.authenticateUser(u, p);
		return b;
	}
	
	public String GetTab1Statistics() {
		return database.CalculateTab1Statistics(cpInformation.T);
	}

	public String GetTab2Statistics() {
		return database.CalculateTab2Statistics();
	}
	
	public String GetLocationStatistics(int x, int y, int R) {
		return database.GetLocationStatistics(x, y, R);
	}
}
