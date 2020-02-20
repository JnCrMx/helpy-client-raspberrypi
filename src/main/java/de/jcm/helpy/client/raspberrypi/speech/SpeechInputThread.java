package de.jcm.helpy.client.raspberrypi.speech;

import de.jcm.helpy.client.raspberrypi.HelpyClient;
import org.mozilla.deepspeech.libdeepspeech.Metadata;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SpeechInputThread extends Thread
{
	private final HelpyClient client;
	private final SpeechToText stt;

	private final AudioFormat recordFormat =
			new AudioFormat(16000, 16, 1,
			true, true);;
	private TargetDataLine microphone;

	private final int BUFFER_SIZE;
	private final int SILENCE_LEVEL;
	private final int SILENCE_TIME;

	private final LinkedList<SpeechListener> speechListeners;

	public SpeechInputThread(HelpyClient client) throws LineUnavailableException
	{
		this.client = client;
		this.stt = client.stt;

		List<Mixer.Info> matchingMixerInfo = findMicrophoneMixers();
		Mixer mixerToUse = AudioSystem.getMixer(matchingMixerInfo.get(0));

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, recordFormat);
		microphone = (TargetDataLine) mixerToUse.getLine(info);

		BUFFER_SIZE =
				client.config.getInt("audio.input.buffer_size", 4096);
		SILENCE_LEVEL =
				client.config.getInt("audio.input.silence_level", 60);
		SILENCE_TIME =
				client.config.getInt("audio.input.silence_time", 10);

		speechListeners = new LinkedList<>();
	}

	public void addSpeechListener(SpeechListener listener)
	{
		speechListeners.add(listener);
	}

	public void removeSpeechListener(SpeechListener listener)
	{
		speechListeners.remove(listener);
	}

	public void removeAllSpeechListeners()
	{
		speechListeners.clear();
	}

	private List<Mixer.Info> findMicrophoneMixers()
	{
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		List<Mixer.Info> matches = new ArrayList<>();
		for (Mixer.Info mixerInfo : mixerInfos)
		{
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class,
					recordFormat);
			boolean isSupported = mixer.isLineSupported(lineInfo);

			if (isSupported)
			{
				matches.add(mixerInfo);
			}
		}

		return matches;
	}

	@Override
	public void run()
	{
		try
		{
			microphone.open(recordFormat);
			microphone.start();

			int silent = 0;
			boolean feed = false;
			while(true)
			{
				byte[] buffer = new byte[BUFFER_SIZE];
				microphone.read(buffer, 0, buffer.length);

				ShortBuffer sb = ByteBuffer.wrap(buffer).asShortBuffer();

				short[] shortBuffer = new short[buffer.length/2];
				sb.get(shortBuffer);

				int level = calculateRMSLevel(buffer);
				if(level<SILENCE_LEVEL)
				{
					silent++;
				}
				else
				{
					feed = true;
					silent = 0;
				}

				if(silent > SILENCE_TIME && feed)
				{
					feed = false;
					Metadata metadata = stt.sttStreamMeta();
					SpeechEvent event = new SpeechEvent(metadata);

					speechListeners.forEach(l->client.executor.execute(()->l.onSpeechEvent(event)));
				}

				if(feed)
				{
					stt.feedStream(shortBuffer);
				}
			}
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
	}

	public int calculateRMSLevel(byte[] audioData)
	{
		long lSum = 0;
		for (byte datum : audioData)
			lSum = lSum + datum;

		double dAvg = ((double)lSum) / ((double)audioData.length);
		double sumMeanSquare = 0d;

		for (byte audioDatum : audioData)
			sumMeanSquare += Math.pow(audioDatum - dAvg, 2d);

		double averageMeanSquare = sumMeanSquare / audioData.length;

		return (int)(Math.pow(averageMeanSquare,0.5d) + 0.5);
	}
}
