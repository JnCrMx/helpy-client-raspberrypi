package de.jcm.helpy.client.raspberrypi.speech;

import javax.sound.sampled.*;
import java.io.IOException;

public class AudioPlayer
{
	private final Clip clip;

	public AudioPlayer() throws LineUnavailableException
	{
		Line.Info linfo = new Line.Info(Clip.class);
		Line line = AudioSystem.getLine(linfo);
		clip = (Clip) line;
	}

	public void play(AudioInputStream in) throws IOException, LineUnavailableException
	{
		clip.stop();
		clip.close();
		clip.open(in);
		clip.start();
	}
}
