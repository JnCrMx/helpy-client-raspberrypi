package de.jcm.helpy.client.raspberrypi.routing;

import de.jcm.helpy.GeoLocation;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.File;

public class OSRMTest
{
	@Test
	void testTarget() throws ConfigurationException
	{
		Configurations configs = new Configurations();
		Configuration config = configs.properties(new File("helpy-client.properties"));

		OSRMApi api = new OSRMApi(config.getString("osrm.url", OSRMApi.BASE_URL));

		api.footRoute(new GeoLocation(), new GeoLocation());
	}
}
