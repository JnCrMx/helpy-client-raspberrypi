package de.jcm.helpy.client.raspberrypi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import de.jcm.helpy.Box;
import de.jcm.helpy.Call;
import de.jcm.helpy.EntityInCallState;
import de.jcm.helpy.api.HelpyApi;
import de.jcm.helpy.api.authentication.StaticTokenProvider;
import de.jcm.helpy.client.raspberrypi.instruction.InstructionPanel;
import de.jcm.helpy.client.raspberrypi.instruction.InstructionUpdateRunnable;
import de.jcm.helpy.client.raspberrypi.navigation.NavigationPanel;
import de.jcm.helpy.client.raspberrypi.routing.OSRMApi;
import de.jcm.helpy.client.raspberrypi.speech.SpeechInputThread;
import de.jcm.helpy.client.raspberrypi.speech.SpeechToText;
import de.jcm.helpy.client.raspberrypi.speech.TextToSpeech;
import de.jcm.helpy.client.raspberrypi.util.ContentUtils;
import de.jcm.helpy.client.raspberrypi.util.UITheme;
import de.jcm.helpy.client.raspberrypi.util.Updater;
import marytts.exceptions.MaryConfigurationException;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class HelpyClient extends JFrame
{
	public static final Configurations configs = new Configurations();

	public final ScheduledExecutorService executor =
			Executors.newSingleThreadScheduledExecutor(runnable ->
			{
				Thread thread = new Thread(runnable);
				thread.setUncaughtExceptionHandler((thread1, throwable) ->
						throwable.printStackTrace());
				return thread;
			});

	public PropertiesConfiguration config;
	public HelpyApi api;
	public Box self;

	public OSRMApi routingApi;

	public TextToSpeech tts;
	public SpeechToText stt;
	public SpeechInputThread speechInput;

	private ScheduledFuture<?> searchFuture;
	private ScheduledFuture<?> instructionFuture;

	private CallAnnouncement announcement;
	public final List<Integer> deniedCalls = new ArrayList<>();

	public Call currentCall;
	public DirectionsRoute currentRoute;
	//TODO: What was the purpose of this?
	public CallPhase phase;

	private Rectangle bounds;
	public UITheme theme;

	public File dataDirectory;
	public Updater updater;

	public ContentUtils contentUtils;

	private final JPanel menuPanel;
	private final JPanel callButtonWrapper;

	public HelpyClient() throws ConfigurationException, IllegalStateException,
			MaryConfigurationException, IOException, LineUnavailableException
	{
		super("HelpyBox");

		init();

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		menuPanel = new JPanel();
		menuPanel.setVisible(false);
		menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.PAGE_AXIS));
		buildMenu();
		panel.add(menuPanel, BorderLayout.WEST);

		JPanel headPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		JButton menuButton = new JButton(new ImageIcon(getClass().getResource("/icons/menu.png")));
		menuButton.setBorderPainted(false);
		menuButton.setFocusPainted(false);
		headPanel.add(menuButton);
		menuButton.addActionListener(e->
									{
		                             	menuPanel.setVisible(!menuPanel.isVisible());
		                             	EventQueue.invokeLater(this::updateMenuWidth);
									});

		panel.add(headPanel, BorderLayout.NORTH);

		callButtonWrapper = new JPanel();
		JButton callButton = new JButton(I18n.translate("call.make"));
		callButton.setFont(theme.createFont(100));
		callButton.addActionListener(e->createCall());
		callButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		callButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		callButtonWrapper.setLayout(new BoxLayout(callButtonWrapper, BoxLayout.Y_AXIS));
		callButtonWrapper.add(javax.swing.Box.createVerticalGlue());
		callButtonWrapper.add(callButton);
		callButtonWrapper.add(javax.swing.Box.createVerticalGlue());
		panel.add(callButtonWrapper, BorderLayout.CENTER);

		setContentPane(panel);
	}

	private void buildMenu()
	{
		menuPanel.removeAll();

		if(currentCall != null)
		{
			JButton endCallButton = new JButton(new AbstractAction(I18n.translate("menu.end_call"))
			{
				@Override
				public void actionPerformed(ActionEvent actionEvent)
				{
					currentCall = null;
					currentRoute = null;

					//TODO: improve this stupid and ugly solution
					HelpyClient.this.getContentPane().remove(getContentPane().getComponentCount()-1);
					HelpyClient.this.getContentPane().add(callButtonWrapper);
					HelpyClient.this.revalidate();
					HelpyClient.this.repaint();

					buildMenu();
					menuPanel.setVisible(false);
				}
			});
			menuPanel.add(endCallButton);
		}

		JButton exitButton = new JButton(new AbstractAction(I18n.translate("menu.exit"))
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				shutdown();
			}
		});
		menuPanel.add(exitButton);
	}

	private void updateMenuWidth()
	{
		Stream.of(menuPanel.getComponents())
		      .forEach(c->c.setSize(new Dimension(menuPanel.getWidth(), c.getHeight())));
	}

	private void createCall()
	{
		currentCall = api.calls().createCall(self.location);
		callNavigationArrived();
	}

	private void init() throws ConfigurationException, MaryConfigurationException, IOException, LineUnavailableException
	{
		dataDirectory = new File(System.getProperty("user.home"), ".helpy");
		dataDirectory.mkdir();

		config = configs.properties(new File(dataDirectory, "helpy-client.properties"));

		I18n.setLocale(Locale.forLanguageTag(config.getString("language", Locale.UK.toLanguageTag())));

		api = new HelpyApi(config.getString("api.url", HelpyApi.BASE_URL));
		if(!api.authenticate(new StaticTokenProvider(config.getString("api.token"))))
			throw new IllegalStateException("not authenticated");

		updater = new Updater(this);
		updater.update();

		self = api.boxes().self();

		routingApi = new OSRMApi(config.getString("osrm.url", OSRMApi.BASE_URL));

		bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getDefaultConfiguration().getBounds();

		tts = new TextToSpeech();
		stt = new SpeechToText();

		//TODO: move somewhere else
		speechInput = new SpeechInputThread(this);
		speechInput.start();
		// Debug
		speechInput.addSpeechListener(System.out::println);

		ObjectMapper mapper = new ObjectMapper();
		InputStream themeIn = getClass().getResourceAsStream("/theme/"+
				config.getString("theme", "dark")+".json");
		theme = mapper.readValue(themeIn, UITheme.class);

		setBackground(theme.background);
		setForeground(theme.foreground);
		try
		{
			if(theme.lookAndFeel.equals("@system@"))
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			else if(theme.lookAndFeel.equals("@cross-platform@"))
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			else
				UIManager.setLookAndFeel(theme.lookAndFeel);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		SwingUtilities.updateComponentTreeUI(this);

		contentUtils = new ContentUtils(this);

		CallSearchRunnable searchRunnable = new CallSearchRunnable(this);
		searchFuture = executor.scheduleWithFixedDelay(searchRunnable, 0, 10, TimeUnit.SECONDS);

		tts.speak(I18n.translate("client.started"));
	}

	private void shutdown()
	{
		if(instructionFuture!=null)
			instructionFuture.cancel(true);
		if(searchFuture!=null)
			searchFuture.cancel(true);

		System.exit(0);
	}

	public void announceCall(Call call, @Nullable DirectionsRoute route)
	{
		if(currentCall==null && !deniedCalls.contains(call.id) && (announcement==null || !announcement.isVisible()))
		{
			System.out.println("Announcing call: " + call);

			tts.speak(I18n.translate("call.announce"));

			announcement = new CallAnnouncement(this, call, route);
			announcement.setLocation(bounds.width/2-announcement.getWidth()/2,
					bounds.height/2-announcement.getHeight()/2);
			announcement.setVisible(true);
		}
	}

	public void denyCall(Call call)
	{
		deniedCalls.add(call.id);

		announcement.setVisible(false);
	}

	public void acceptCall(Call call, DirectionsRoute route)
	{
		currentCall = call;
		currentRoute = route;
		phase = CallPhase.NAVIGATION;

		api.calls().join(currentCall, EntityInCallState.INCOMING);

		announcement.setVisible(false);

		//TODO: improve this stupid and ugly solution
		getContentPane().remove(getContentPane().getComponentCount()-1);
		getContentPane().add(new NavigationPanel(this), BorderLayout.CENTER);
		revalidate();
		repaint();

		buildMenu();
	}

	public void callNavigationArrived()
	{
		api.calls().join(currentCall, EntityInCallState.PRESENT);

		InstructionPanel instructionPanel = new InstructionPanel(this);

		//TODO: improve this stupid and ugly solution
		getContentPane().remove(getContentPane().getComponentCount()-1);
		getContentPane().add(instructionPanel, BorderLayout.CENTER);
		revalidate();
		repaint();

		instructionFuture = executor.scheduleWithFixedDelay(
				new InstructionUpdateRunnable(this, instructionPanel), 0, 1, TimeUnit.SECONDS);

		buildMenu();
	}

	public static void main(String[] args)
	{
		EventQueue.invokeLater(()->
		{
			HelpyClient client = null;
			try
			{
				client = new HelpyClient();
				client.setVisible(true);
			}
			catch (ConfigurationException | MaryConfigurationException | IOException | LineUnavailableException e)
			{
				e.printStackTrace();
			}

			GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().setFullScreenWindow(client);
		});
	}
}
