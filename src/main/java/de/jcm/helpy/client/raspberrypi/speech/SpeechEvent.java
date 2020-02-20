package de.jcm.helpy.client.raspberrypi.speech;

import org.mozilla.deepspeech.libdeepspeech.Metadata;

public class SpeechEvent
{
    private Metadata metadata;
    private String result;
    private double confidence;

    public SpeechEvent(Metadata metadata)
    {
        this.metadata = metadata;

        StringBuilder builder = new StringBuilder();
        for(int i=0; i<metadata.getNum_items(); i++)
        {
            builder.append(metadata.getItem(i).getCharacter());
        }
        result = builder.toString();

        confidence = metadata.getConfidence();
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public String getResult()
    {
        return result;
    }

    @Override
    public String toString()
    {
        return "SpeechEvent{" +
                "result='" + result + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
