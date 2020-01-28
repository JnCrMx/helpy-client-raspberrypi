package de.jcm.helpy.client.raspberrypi.instruction;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import de.jcm.helpy.client.raspberrypi.I18n;
import de.jcm.helpy.content.ContentForm;
import de.jcm.helpy.content.ContentOption;
import de.jcm.helpy.content.ContentPage;
import uk.co.caprica.vlcj.media.*;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

public class InstructionPanel extends JPanel
{
	// File handling stuff
	private HelpyClient client = null;

	private File contentDirectory;

	private ContentPage currentPage;
	private File currentFile;

	// Swing components
	private JLabel shortMessageLabel;

	private JPanel helpTextPanel;
	private JLabel[] helpTextLabels;

	private JLabel imageLabel;
	private EmbeddedMediaPlayerComponent mediaPlayerComponent;

	private JPanel optionButtonPanel;
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

			JPanel mediaPanel = new JPanel();
			mainPanel.add(mediaPanel, BorderLayout.CENTER);
			{
				imageLabel = new JLabel();
				imageLabel.setVisible(false);
				mediaPanel.add(imageLabel, BorderLayout.CENTER);

				mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
				mediaPlayerComponent.setVisible(false);
				mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);
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

				System.err.println("Load page \""+path+"\" from \""+currentFile.getAbsolutePath()+"\"");

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

			loadPage(client.contentUtils.getTarget(oldPath, button.getOption().target));
		}
	};

	private void refreshPage()
	{
		shortMessageLabel.setText("<html><center>"+currentPage.shortMessage+"</center></html>");

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

		imageLabel.setVisible(false);
		mediaPlayerComponent.setVisible(false);
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

				imageLabel.setVisible(true);
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
				MediaRef media = mediaPlayerComponent.mediaPlayerFactory()
						.media().newMediaRef(videoFile.get().getAbsolutePath());
				mediaPlayerComponent.mediaPlayer().media().play(media);
				mediaPlayerComponent.mediaPlayer().controls().play();

				mediaPlayerComponent.setVisible(true);
				controlButton.setVisible(true);
			}
			else
			{
				System.err.println("Could not find video file \""+currentPage.video+
						"\" from content page \""+currentFile.getAbsolutePath()+"\"!");
			}
		}
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
