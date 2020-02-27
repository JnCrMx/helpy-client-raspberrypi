package de.jcm.helpy.client.raspberrypi.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import de.jcm.helpy.hardware.HelpyHardware;

public class RaspberryPiHardware implements HelpyHardware
{
	private final GpioController gpio = GpioFactory.getInstance();
	private final HardwareConfiguration configuration;

	public RaspberryPiHardware(HardwareConfiguration configuration)
	{
		this.configuration = configuration;
	}

	@Override
	public void startHighlightElement(String s)
	{
		Pin pin = configuration.getHighlightPin(s);
		GpioPinDigitalOutput gpioPinDigitalOutput = gpio.provisionDigitalOutputPin(pin);
		gpioPinDigitalOutput.high();
	}

	@Override
	public void stopHighlightElement(String s)
	{
		Pin pin = configuration.getHighlightPin(s);
		GpioPinDigitalOutput gpioPinDigitalOutput = gpio.provisionDigitalOutputPin(pin);
		gpioPinDigitalOutput.low();
	}

	@Override
	public byte[] readSensorData(String s)
	{
		return new byte[0];
	}
}
