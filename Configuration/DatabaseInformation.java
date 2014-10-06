package Configuration;

import java.util.Properties;

public class DatabaseInformation {
	public final String username;
	public final String password;
	public final String ip;
	public final String port;
	public final String database_name;
	public final int R;
	public final int T;									// mikro kai oxi milli
	
	public DatabaseInformation(Properties p) {
		username = p.getProperty("username");
		password = p.getProperty("password");
		ip = p.getProperty("ip");
		port = p.getProperty("port");
		database_name = p.getProperty("database_name");
		R = Integer.parseInt(p.getProperty("R"));
		T = 1000000* Integer.parseInt(p.getProperty("T"));
	
		if (username == null || password == null || ip == null || port == null) {
			throw new IllegalArgumentException("Invalid properties file or settings missing");
		}		
	}
	
	public String getDatabaseURL() {
		return  "jdbc:mysql://" + ip + ":" + port + "/" + database_name;
	}

	@Override
	public String toString() {
		return "DatabaseInformation [username=" + username + ", password="
				+ password + ", ip=" + ip + ", port=" + port
				+ ", database_name=" + database_name + ", R=" + R + ", T=" + T
				+ "]";
	}
}
