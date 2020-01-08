package de.jcm.helpy.client.raspberrypi.routing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import de.jcm.helpy.GeoLocation;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class OSRMApi
{
	public static final String BASE_URL = "https://jserver.kwgivnecuiphlqnj.myfritz.net/osrm/route/v1/";

	protected final Client client;
	protected final WebTarget baseTarget;

	protected final WebTarget footTarget;

	public OSRMApi(String baseUrl)
	{
		// OSRM gives us more data than DirectionsResponse can actually handle
		JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		client = ClientBuilder.newClient(new ClientConfig(jacksonJsonProvider));
		baseTarget = client.target(baseUrl);

		footTarget = baseTarget.path("foot");
	}

	public OSRMApi()
	{
		this(BASE_URL);
	}

	public DirectionsResponse footRoute(GeoLocation start, GeoLocation end)
	{
		String desc = start.longitude + "," + start.latitude + ";" +
				end.longitude + "," + end.latitude;
		WebTarget requestTarget = footTarget.path(desc)
				.queryParam("steps", true);

		return requestTarget.request(MediaType.APPLICATION_JSON)
				.get(DirectionsResponse.class);
	}
}
