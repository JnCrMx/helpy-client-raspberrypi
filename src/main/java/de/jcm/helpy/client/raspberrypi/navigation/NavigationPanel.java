package de.jcm.helpy.client.raspberrypi.navigation;

import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.utils.PolylineUtils;
import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.client.raspberrypi.I18n;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.AwtUtil;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NavigationPanel extends JPanel
{
	public NavigationPanel(HelpyClient client)
	{
		setLayout(new BorderLayout());

		JPanel northPanel = new JPanel();

		JButton arrivedButton = new JButton(I18n.translate("navigation.arrived"));
		arrivedButton.setFont(client.theme.createFont(50));
		arrivedButton.addActionListener(e->client.callNavigationArrived());
		northPanel.add(arrivedButton);

		add(northPanel, BorderLayout.NORTH);

		DirectionsRoute route = client.currentRoute;

		if (route != null)
		{
			List<LatLong> points = new ArrayList<>();
			route.getLegs().get(0).getSteps().forEach(s ->
					PolylineUtils.decode(s.getGeometry(), 5)
							.forEach(p -> points.add(new LatLong(p.getLatitude(), p.getLongitude()))));

			MapView mapView = new MapView();
			TileCache tileCache = AwtUtil.createTileCache(
					256,
					mapView.getModel().frameBufferModel.getOverdrawFactor(),
					1024,
					new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()));

			OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
			tileSource.setUserAgent("mapsforge-samples-awt");
			TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache,
					mapView.getModel().mapViewPosition,
					tileSource, AwtGraphicFactory.INSTANCE);
			mapView.addLayer(tileDownloadLayer);
			tileDownloadLayer.start();

			mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
			mapView.setZoomLevelMax(tileSource.getZoomLevelMax());

			BoundingBox boundingBox = new BoundingBox(points);

			org.mapsforge.core.graphics.Paint paint = AwtGraphicFactory.INSTANCE.createPaint();
			paint.setColor(org.mapsforge.core.graphics.Color.RED);
			paint.setStrokeWidth(5.0f);
			paint.setStyle(Style.STROKE);
			paint.setStrokeJoin(Join.ROUND);
			paint.setStrokeCap(Cap.ROUND);

			Polyline polyline = new Polyline(paint, AwtGraphicFactory.INSTANCE);
			polyline.addPoints(points);
			mapView.addLayer(polyline);

			mapView.setPreferredSize(new Dimension(500, 500));
			add(mapView, BorderLayout.CENTER);

			Duration duration = Duration.ofMillis((long) (route.getDuration() * 1000.0));

			JLabel durationLabel = new JLabel(I18n.translate("navigation.duration",
					duration.toMinutesPart(), duration.toSecondsPart()));
			durationLabel.setFont(client.theme.createFont(20));

			JLabel distanceLabel = new JLabel(I18n.translate("navigation.distance", route.getDistance()));
			distanceLabel.setFont(client.theme.createFont(20));

			JPanel routeInfoPanel = new JPanel();
			routeInfoPanel.setLayout(new BoxLayout(routeInfoPanel, BoxLayout.LINE_AXIS));
			routeInfoPanel.add(Box.createHorizontalGlue());
			routeInfoPanel.add(durationLabel);
			routeInfoPanel.add(Box.createHorizontalGlue());
			routeInfoPanel.add(distanceLabel);
			routeInfoPanel.add(Box.createHorizontalGlue());

			add(routeInfoPanel, BorderLayout.SOUTH);

			tileCache.addObserver(new Observer()
			{
				boolean once = true;

				@Override
				public void onChange()
				{
					if(once)
					{
						once = false;
						EventQueue.invokeLater(() ->
						{
							Model model = mapView.getModel();
							if (model.mapViewPosition.getZoomLevel() == 0 ||
									!boundingBox.contains(model.mapViewPosition.getCenter()))
							{
								byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(),
										boundingBox, model.displayModel.getTileSize());
								model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(),
										zoomLevel));
							}
						});
					}
				}
			});
		}
	}
}
