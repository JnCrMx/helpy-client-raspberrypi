package de.jcm.helpy.client.raspberrypi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;

public class ThemeTest
{
	@Test
	void testThemeSerialization() throws IOException
	{
		UITheme theme1 = new UITheme();
		theme1.background = Color.DARK_GRAY;
		theme1.foreground = Color.WHITE;
		theme1.fontFamily = "Arial";
		theme1.lookAndFeel = "@system@";

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(theme1);

		System.out.println(json);

		UITheme theme2 = mapper.readValue(json, UITheme.class);

		Assertions.assertEquals(theme1, theme2);
	}
}
