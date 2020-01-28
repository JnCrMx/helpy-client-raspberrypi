package de.jcm.helpy.client.raspberrypi.speech;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.util.Locale;

public class MaryTest
{
	@Test
	void testTTS() throws MaryConfigurationException, SynthesisException, LineUnavailableException, IOException
	{
		MaryInterface mary = new LocalMaryInterface();
		mary.setLocale(Locale.forLanguageTag(Locale.GERMANY.getLanguage()));
		
		AudioInputStream ais = mary.generateAudio("Hello World! This is just a test!");

		AudioPlayer player = new AudioPlayer();
		player.play(ais);
	}
}
