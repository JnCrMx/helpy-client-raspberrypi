package de.jcm.helpy.client.raspberrypi.instruction;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.client.raspberrypi.I18n;
import de.jcm.helpy.content.ContentForm;
import de.jcm.helpy.content.ContentOption;
import de.jcm.helpy.content.ContentPage;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

public class InstructionPanel extends JPanel
{
	// File handling stuff
	private HelpyClient client;

	private final File contentDirectory;

	private ContentPage currentPage;
	private File currentFile;

	// Swing components
	private final JLabel shortMessageLabel;

	private final JPanel helpTextPanel;
	private JLabel[] helpTextLabels;

	private final JPanel mediaPanel;
	private final JLabel imageLabel;
	private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private JButton controlButton;
	private final JPanel videoControlPanel;

	private final JPanel optionButtonPanel;
	private JButton[] optionButtons;

	public InstructionPanel(HelpyClient client)
	{
		this.client = client;

		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(mainPanel, BorderLayout.CENTER);
		{
			JPanel instructionPanel = new JPanel(new BorderLayout());
			mainPanel.add(instructionPanel, BorderLayout.NORTH);
			{
				shortMessageLabel = new JLabel();
				shortMessageLabel.setHorizontalAlignment(SwingConstants.CENTER);
				shortMessageLabel.setFont(client.theme.createFont(50));
				instructionPanel.add(shortMessageLabel, BorderLayout.NORTH);

				helpTextPanel = new JPanel();
				helpTextPanel.setLayout(new BoxLayout(helpTextPanel, BoxLayout.PAGE_AXIS));
				instructionPanel.add(helpTextPanel, BorderLayout.CENTER);
			}

			mediaPanel = new JPanel(new BorderLayout());
			mainPanel.add(mediaPanel, BorderLayout.CENTER);
			{
				imageLabel = new JLabel("", JLabel.CENTER);

				mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
				mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter()
				{
					@Override
					public void playing(MediaPlayer mediaPlayer)
					{
						controlButton.setText(I18n.translate("instructions.video.pause"));
						controlButton.setActionCommand("pause");
					}

					@Override
					public void paused(MediaPlayer mediaPlayer)
					{
						controlButton.setText(I18n.translate("instructions.video.continue"));
						controlButton.setActionCommand("continue");
					}

					@Override
					public void finished(MediaPlayer mediaPlayer)
					{
						controlButton.setText(I18n.translate("instructions.video.play_again"));
						controlButton.setActionCommand("replay");
					}
				});

				controlButton = new JButton();
				controlButton.setFont(client.theme.createFont(30));
				controlButton.addActionListener(e->
				{
					if(e.getActionCommand().equals("pause"))
					{
						mediaPlayerComponent.mediaPlayer().controls().pause();
					}
					if(e.getActionCommand().equals("continue"))
					{
						mediaPlayerComponent.mediaPlayer().controls().play();
					}
					if(e.getActionCommand().equals("replay"))
					{
						mediaPlayerComponent.mediaPlayer().controls().play();
					}
				});
				videoControlPanel = new JPanel(new FlowLayout());
				videoControlPanel.add(controlButton);
			}

			optionButtonPanel = new JPanel();
			optionButtonPanel.setLayout(new BoxLayout(optionButtonPanel, BoxLayout.LINE_AXIS));
			optionButtonPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			mainPanel.add(optionButtonPanel, BorderLayout.SOUTH);
		}
		contentDirectory = new File(client.updater.contentDirectory, I18n.getLocale().toLanguageTag());

		addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent ancestorEvent)
			{
				loadPage("index");
			}

			@Override
			public void ancestorRemoved(AncestorEvent ancestorEvent)
			{

			}

			@Override
			public void ancestorMoved(AncestorEvent ancestorEvent)
			{

			}
		});
	}

	public void loadPage(String path)
	{
		try
		{
			File newFile = new File(contentDirectory, path+".json");
			// Only load page if it isn't already loaded.
			if(currentFile==null || !currentFile.getAbsolutePath().equals(newFile.getAbsolutePath()))
			{
				currentFile = newFile;

				System.out.println("Load page \""+path+"\" from \""+currentFile.getAbsolutePath()+"\"");

				currentPage = client.contentUtils.readPage(path);
				refreshPage();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private final ActionListener optionButtonActionListener = e->
	{
		if(e.getSource() instanceof OptionButton)
		{
			OptionButton button = (OptionButton) e.getSource();

			String oldPath = client.contentUtils.fileToPath(currentFile);
			client.api.calls().addCallInteraction(client.currentCall, currentPage, oldPath, button.getOption());

			if(button.getOption().message!=null && !button.getOption().message.isBlank())
			{
				client.tts.speak(button.getOption().message);
				popup(button.getOption().message);
			}

			if(button.getOption().target!=null && !button.getOption().target.isBlank())
			{
				loadPage(client.contentUtils.getTarget(oldPath, button.getOption().target));
			}
			else
			{
				//TODO: end instruction flow here
			}
		}
	};

	public void popup(String message)
	{
		EventQueue.invokeLater(()->
		{
			JOptionPane.showMessageDialog(null, message);
		});
	}

	private void refreshPage()
	{
		shortMessageLabel.setText("<html><center>"+currentPage.shortMessage+"</center></html>");
		//TODO: move somewhere else maybe?
		client.tts.speak(currentPage.shortMessage);

		optionButtonPanel.removeAll();
		if(currentPage.form == ContentForm.PREDICAMENT || currentPage.form == ContentForm.CONTINUE)
		{
			optionButtons = new JButton[currentPage.options.length];
			optionButtonPanel.add(Box.createHorizontalGlue());  // center horizontally
			for(int i=0; i<currentPage.options.length; i++)
			{
				optionButtons[i] = new OptionButton(currentPage.options[i]);
				optionButtons[i].addActionListener(optionButtonActionListener);

				optionButtonPanel.add(optionButtons[i]);
				if((i + 1) < currentPage.options.length)
					optionButtonPanel.add(Box.createHorizontalStrut(50));
			}
			optionButtonPanel.add(Box.createHorizontalGlue());
		}
		optionButtonPanel.revalidate();
		optionButtonPanel.repaint();

		mediaPanel.removeAll();
		mediaPlayerComponent.mediaPlayer().controls().stop();
		if(currentPage.image!=null)
		{
			// all possible locations to search for assets in
			Stream<File> locations = Stream.of(
					new File(currentFile.getParentFile(), currentPage.image),
					new File(contentDirectory, currentPage.image),
					new File(client.updater.contentDirectory, currentPage.image));

			Optional<File> imageFile = locations.filter(File::exists).findFirst();
			if(imageFile.isPresent())
			{
				imageLabel.setIcon(new ImageIcon(imageFile.get().getAbsolutePath()));

				mediaPanel.add(imageLabel, BorderLayout.CENTER);
			}
			else if(currentPage.image.startsWith("http"))
			{
				try
				{
					URL url = new URL(currentPage.image);
					Image img = ImageIO.read(url);
					imageLabel.setIcon(new ImageIcon(img));

					mediaPanel.add(imageLabel, BorderLayout.CENTER);
				}
				catch (MalformedURLException e)
				{
					e.printStackTrace();
					System.err.println("Could not find image file \""+currentPage.image+
							"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.err.println("Could not read image from \""+currentPage.image+
							"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
				}
			}
			else
			{
				System.err.println("Could not find image file \""+currentPage.image+
						"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
			}
		}
		else if(currentPage.video!=null)
		{
			// all possible locations to search for assets in
			Stream<File> locations = Stream.of(
					new File(currentFile.getParentFile(), currentPage.video),
					new File(contentDirectory, currentPage.video),
					new File(client.updater.contentDirectory, currentPage.video));

			Optional<File> videoFile = locations.filter(File::exists).findFirst();
			if(videoFile.isPresent())
			{
				mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);
				mediaPanel.add(videoControlPanel, BorderLayout.SOUTH);

				MediaRef media = mediaPlayerComponent.mediaPlayerFactory()
						.media().newMediaRef(videoFile.get().getAbsolutePath());
				mediaPlayerComponent.mediaPlayer().media().play(media);
				mediaPlayerComponent.mediaPlayer().controls().play();
			}
			else if(currentPage.video.startsWith("https://www.youtube.com/watch?v="))
			{
				try
				{
					Process process = new ProcessBuilder()
							.command("youtube-dl", "-f", "best[ext=mp4]", "-g", currentPage.video)
							.redirectError(ProcessBuilder.Redirect.INHERIT)
							.start();
					int code;
					if((code = process.waitFor())==0)
					{
						String url = new String(process.getInputStream().readAllBytes());
						System.out.println("YouTube-Video url \""+currentPage.video+
								"\" from content page \""+currentFile.getAbsolutePath()+
								"\" resolved to \""+url+"\"!");

						mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);
						mediaPanel.add(videoControlPanel, BorderLayout.SOUTH);

						MediaRef media = mediaPlayerComponent.mediaPlayerFactory()
								.media().newMediaRef(url);
						mediaPlayerComponent.mediaPlayer().media().play(media);
						mediaPlayerComponent.mediaPlayer().controls().play();
					}
					else
					{
						System.err.println("youtube-dl failed for video \""+currentPage.video+
								"\" from content page \""+currentFile.getAbsolutePath()+
								"\" with exit code "+code+"!");
					}
				}
				catch (IOException | InterruptedException e)
				{
					e.printStackTrace();
					System.err.println("Could not invoke youtube-dl for video \""+currentPage.video+
							"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
				}
			}
			else if(currentPage.video.startsWith("http"))   // just hope for it being readable by VLC
			{
				mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);
				mediaPanel.add(videoControlPanel, BorderLayout.SOUTH);

				MediaRef media = mediaPlayerComponent.mediaPlayerFactory()
						.media().newMediaRef(currentPage.video);
				mediaPlayerComponent.mediaPlayer().media().play(media);
				mediaPlayerComponent.mediaPlayer().controls().play();
			}
			else
			{
				System.err.println("Could not find video file \""+currentPage.video+
						"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
			}
		}
		mediaPanel.revalidate();
		mediaPanel.repaint();
	}

	private class OptionButton extends JButton
	{
		private final ContentOption option;

		public OptionButton(ContentOption option)
		{
			super(option.answers[0]);

			this.option = option;

			setFont(client.theme.createFont(50));
			setMargin(new Insets(10, 10, 10, 10));
		}

		public ContentOption getOption()
		{
			return option;
		}
	}
}
