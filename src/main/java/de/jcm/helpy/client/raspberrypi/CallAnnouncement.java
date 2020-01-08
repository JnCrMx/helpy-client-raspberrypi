package de.jcm.helpy.client.raspberrypi;

import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.utils.PolylineUtils;
import de.jcm.helpy.Call;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CallAnnouncement extends JDialog
{
	private final Call call;
	private final DirectionsRoute route;

	public CallAnnouncement(HelpyClient client, Call call, @Nullable DirectionsRoute route)
	{
		this.call = call;
		this.route = route;

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

		JLabel callLabel = new JLabel(I18n.translate("call.announce"));
		callLabel.setFont(new Font("Arial", Font.BOLD, 50));
		callLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(callLabel);

		if(route!=null)
		{
			List<LatLong> points = new ArrayList<>();
			route.getLegs().get(0).getSteps().forEach(s ->
			{
				PolylineUtils.decode(s.getGeometry(), 5)
						.forEach(p->points.add(new LatLong(p.getLatitude(), p.getLongitude())));
			});

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

			addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowOpened(WindowEvent e)
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
				}
			});

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
			contentPane.add(mapView);

			Duration duration = Duration.ofMillis((long) (route.getDuration()*1000.0));

			JLabel durationLabel = new JLabel(I18n.translate("route.duration",
					duration.toMinutesPart(), duration.toSecondsPart()));
			durationLabel.setForeground(Color.WHITE);
			durationLabel.setFont(new Font("Arial", Font.PLAIN, 20));

			JLabel distanceLabel = new JLabel(I18n.translate("route.distance", route.getDistance()));
			distanceLabel.setForeground(Color.WHITE);
			distanceLabel.setFont(new Font("Arial", Font.PLAIN, 20));

			JPanel routeInfoPanel = new JPanel();
			routeInfoPanel.setLayout(new BoxLayout(routeInfoPanel, BoxLayout.LINE_AXIS));
			routeInfoPanel.add(Box.createHorizontalGlue());
			routeInfoPanel.add(durationLabel);
			routeInfoPanel.add(Box.createHorizontalGlue());
			routeInfoPanel.add(distanceLabel);
			routeInfoPanel.add(Box.createHorizontalGlue());

			contentPane.add(routeInfoPanel);
		}

		JButton acceptButton = new JButton(I18n.translate("call.accept"));
		acceptButton.setForeground(Color.GREEN);
		acceptButton.setFont(new Font("Arial", Font.BOLD, 25));
		acceptButton.addActionListener(e->client.acceptCall(call, route));

		JButton denyButton = new JButton(I18n.translate("call.deny"));
		denyButton.setForeground(Color.RED);
		denyButton.setFont(new Font("Arial", Font.BOLD, 25));
		denyButton.addActionListener(e->client.denyCall(call));

		JPanel acceptDenyPanel = new JPanel();
		acceptDenyPanel.add(acceptButton);
		acceptDenyPanel.add(denyButton);
		acceptDenyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(acceptDenyPanel);

		setContentPane(contentPane);
		pack();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setAlwaysOnTop(true);
	}
}
