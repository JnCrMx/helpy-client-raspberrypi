package de.jcm.helpy.client.raspberrypi.speech;

import de.jcm.helpy.client.raspberrypi.I18n;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

import javax.sound.sampled.AudioInputStream;

public class TextToSpeech
{
	private MaryInterface mary;
	private AudioPlayer audioPlayer;

	public TextToSpeech() throws MaryConfigurationException
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
			audioPlayer.setAudio(in);
			audioPlayer.start();
		}
		catch (SynthesisException e)
		{
			e.printStackTrace();
		}

	}
}
