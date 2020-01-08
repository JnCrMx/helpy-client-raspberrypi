package de.jcm.helpy.client.raspberrypi.speech;

import de.jcm.helpy.client.raspberrypi.I18n;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class DeepSpeechTest
{
	private char readLEChar(RandomAccessFile f) throws IOException
	{
		byte b1 = f.readByte();
		byte b2 = f.readByte();
		return (char)((b2 << 8) | b1);
	}

	private int readLEInt(RandomAccessFile f) throws IOException
	{
		byte b1 = f.readByte();
		byte b2 = f.readByte();
		byte b3 = f.readByte();
		byte b4 = f.readByte();
		return (b1 & 0xFF) | (b2 & 0xFF) << 8 | (b3 & 0xFF) << 16 | (b4 & 0xFF) << 24;
	}

	private final SpeechToText stt;

	DeepSpeechTest() throws IOException
	{
		// We sadly need to do this in order to integrate into not existing context.
		I18n.setLocale(Locale.GERMANY);

		stt = new SpeechToText();
	}

	@Test
	void singleTest() throws IOException
	{
		RandomAccessFile wave = new RandomAccessFile(
				"/home/jcm/Projekte/helpy/HelpySpeech/clips/20191207203700.wav", "r");

		wave.seek(20); char audioFormat = this.readLEChar(wave);
		assert (audioFormat == 1); // 1 is PCM

		wave.seek(22); char numChannels = this.readLEChar(wave);
		assert (numChannels == 1); // MONO

		wave.seek(24); int sampleRate = this.readLEInt(wave);
		assert (sampleRate == stt.getModel().sampleRate()); // desired sample rate

		wave.seek(34); char bitsPerSample = this.readLEChar(wave);
		assert (bitsPerSample == 16); // 16 bits per sample

		wave.seek(40); int bufferSize = this.readLEInt(wave);
		assert (bufferSize > 0);

		wave.seek(44);
		byte[] bytes = new byte[bufferSize];
		wave.readFully(bytes);

		short[] shorts = new short[bytes.length/2];
		// to turn bytes to shorts as either big endian or little endian.
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

		System.out.println("shorts.length = " + shorts.length);
		System.out.println("duration = " + (shorts.length*1000) / sampleRate);

		long inferenceStartTime = System.currentTimeMillis();

		String decoded = stt.sttStream(shorts);

		long inferenceExecTime = System.currentTimeMillis() - inferenceStartTime;

		System.out.println("decoded = " + decoded);
		System.out.println("inferenceExecTime = " + inferenceExecTime);
	}

	@Test
	void streamTest() throws IOException
	{
		RandomAccessFile wave = new RandomAccessFile(
				"/home/jcm/Projekte/helpy/HelpySpeech/clips/20191207203700.wav", "r");

		wave.seek(20); char audioFormat = this.readLEChar(wave);
		assert (audioFormat == 1); // 1 is PCM

		wave.seek(22); char numChannels = this.readLEChar(wave);
		assert (numChannels == 1); // MONO

		wave.seek(24); int sampleRate = this.readLEInt(wave);
		assert (sampleRate == stt.getModel().sampleRate()); // desired sample rate

		wave.seek(34); char bitsPerSample = this.readLEChar(wave);
		assert (bitsPerSample == 16); // 16 bits per sample

		wave.seek(40); int bufferSize = this.readLEInt(wave);
		assert (bufferSize > 0);

		wave.seek(44);
		byte[] bytes = new byte[bufferSize];
		wave.readFully(bytes);

		short[] shorts = new short[bytes.length/2];
		// to turn bytes to shorts as either big endian or little endian.
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

		System.out.println("shorts.length = " + shorts.length);
		System.out.println("duration = " + (shorts.length*1000) / sampleRate);

		long inferenceStartTime = System.currentTimeMillis();

		stt.feedStream(shorts);
		String decoded = stt.sttStream();

		long inferenceExecTime = System.currentTimeMillis() - inferenceStartTime;

		System.out.println("decoded = " + decoded);
		System.out.println("inferenceExecTime = " + inferenceExecTime);
	}
}
