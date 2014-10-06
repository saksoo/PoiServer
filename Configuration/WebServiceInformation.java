package Configuration;

import java.util.Properties;

public class WebServiceInformation {
	public final String port;
	public final String subfolder;
	public final String ip;
	
	public WebServiceInformation(Properties p) {
		port = p.getProperty("ws_port");
		subfolder = p.getProperty("ws_subfolder");
		ip =  p.getProperty("ws_ip");
	
		if (port == null || subfolder == null || ip == null) {
			throw new IllegalArgumentException("Invalid properties file or settings missing");
		}	
	}

	@Override
	public String toString() {
		return "WebServiceInformation [port=" + port + ", subfolder="
				+ subfolder + ", ip=" + ip + "]";
	}
}
