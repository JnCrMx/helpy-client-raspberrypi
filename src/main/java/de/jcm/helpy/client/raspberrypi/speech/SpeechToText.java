package de.jcm.helpy.client.raspberrypi.speech;

import de.jcm.helpy.client.raspberrypi.I18n;
import de.jcm.helpy.client.raspberrypi.util.ResourceUtils;
import org.mozilla.deepspeech.libdeepspeech.DeepSpeechModel;
import org.mozilla.deepspeech.libdeepspeech.DeepSpeechStreamingState;

import java.io.File;
import java.io.IOException;

public class SpeechToText
{
	private final DeepSpeechModel model;
	private final DeepSpeechStreamingState stream;

	public SpeechToText() throws IOException
	{
		String base = "/model/"+I18n.getLanguage().toLanguageTag();

		File modelFile;
		if(System.getProperty("os.arch").equals("arm"))
			modelFile = ResourceUtils.extractResource(base+"/output_graph.tflite");
		else
			modelFile = ResourceUtils.extractResource(base+"/output_graph.pb");
		File lmFile = ResourceUtils.extractResource(base+"/lm.binary");
		File trieFile = ResourceUtils.extractResource(base+"/trie");

		// We choose the default beam_width from DeepSpeech.py here.
		model = new DeepSpeechModel(modelFile.getAbsolutePath(),500);
		// and here it is the default values from native_client (defined in args.h)
		model.enableDecoderWihLM(lmFile.getAbsolutePath(),trieFile.getAbsolutePath(),
				0.75f, 1.85f);
		stream = model.createStream();
	}

	public String sttStream(short[] buffer)
	{
		return model.stt(buffer, buffer.length);
	}

	public void feedStream(short[] buffer)
	{
		model.feedAudioContent(stream, buffer, buffer.length);
	}

	public String sttStream()
	{
		return model.intermediateDecode(stream);
	}

	public DeepSpeechModel getModel()
	{
		return model;
	}
}
