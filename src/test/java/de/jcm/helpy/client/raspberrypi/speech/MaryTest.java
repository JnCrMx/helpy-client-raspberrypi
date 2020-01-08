package de.jcm.helpy.client.raspberrypi.speech;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioInputStream;
import java.util.Locale;

public class MaryTest
{
	@Test
	void testTTS() throws MaryConfigurationException, SynthesisException
	{
		MaryInterface mary = new LocalMaryInterface();
		mary.setLocale(Locale.forLanguageTag(Locale.GERMANY.getLanguage()));
		
		AudioInputStream ais = mary.generateAudio("Hello World! This is just a test!");

		AudioPlayer player = new AudioPlayer(ais);
		/*
		 We use Thread#run() instead of Thread#start(), because we actually - well - don't want to start
		 a new Thread.
		 This is a test unit and starting a new Thread would cause this method to return and probably
		 end the test.
		 Therefore we want to play the audio in our current thread.
		 */
		player.run();
	}
}
