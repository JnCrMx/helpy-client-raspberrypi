package de.jcm.helpy.client.raspberrypi;

import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import de.jcm.helpy.Call;

import java.time.LocalDateTime;
import java.util.Comparator;

public class CallSearchRunnable implements Runnable
{
	private HelpyClient client;

	public CallSearchRunnable(HelpyClient client)
	{
		this.client = client;
	}

	@Override
	public void run()
	{
		Call[] calls = client.api.calls().inRange(client.self.location, 10000);

		for (Call call:calls)
		{
			if(call.time.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(1))
				&& call.time.toLocalDateTime().isBefore(LocalDateTime.now()))
			{
				DirectionsRoute route;
				try
				{
					DirectionsResponse response =
							client.routingApi.footRoute(client.self.location, call.location);
					// Search for fastest route
					route = response.getRoutes().stream()
							.min(Comparator.comparingDouble(DirectionsRoute::getDuration))
							.orElse(null);
				}
				catch (Throwable t)
				{
					t.printStackTrace();
					route = null;
				}
				client.announceCall(call, route);
			}
		}
	}
}
