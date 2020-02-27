package de.jcm.helpy.client.raspberrypi.hardware;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import de.jcm.helpy.client.raspberrypi.HelpyClient;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class HardwareConfiguration
{
	private PropertiesConfiguration highlightConfiguration;

	public HardwareConfiguration(File directory)
	{
		File highlightConfigurationFile = new File(directory, "highlight.yml");
		try
		{
			highlightConfiguration = HelpyClient.configs.properties(highlightConfigurationFile);
		}
		catch(ConfigurationException e)
		{
			e.printStackTrace();
		}
	}

	public Pin getHighlightPin(String element)
	{
		return RaspiPin.GPIO_06;
	}
}
