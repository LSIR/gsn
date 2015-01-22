package gsn.http.restapi;


import java.util.HashSet;
import java.util.Set;

//import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {
	
	public RestApplication(){
		packages("gsn.http.restapi");
	}
	/*@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(RestServices.class);
		return s;
	}*/	
	
}
