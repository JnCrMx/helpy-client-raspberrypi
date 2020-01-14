package de.jcm.helpy.client.raspberrypi.speech;

import de.jcm.helpy.client.raspberrypi.I18n;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;

public class TextToSpeech
{
	private final MaryInterface mary;
	private final AudioPlayer audioPlayer;

	public TextToSpeech() throws MaryConfigurationException, LineUnavailableException
	{
		mary = new LocalMaryInterface();
		mary.setLocale(I18n.getLanguage());

		audioPlayer = new AudioPlayer();
	}

	public void speak(String text)
	{
		try
		{
			AudioInputStream in = mary.generateAudio(text);
			audioPlayer.play(in);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}

	}
}
